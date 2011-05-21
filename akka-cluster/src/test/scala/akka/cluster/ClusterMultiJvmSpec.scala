/**
 *  Copyright (C) 2009-2011 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.cluster

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }

import akka.cluster.zookeeper._
import org.I0Itec.zkclient._

object MultiNodeTest {
  val NrOfNodes = 2
  val ClusterName = "test-cluster"
  val DataPath = "_akka_cluster/data"
  val LogPath = "_akka_cluster/log"
}

trait MultiNodeTest extends WordSpec with MustMatchers with BeforeAndAfterAll with BeforeAndAfterEach {
  import MultiNodeTest._

  val nodeNr = nodeNumber
  val port = 9000 + nodeNumber

  var zkServer: ZkServer = _
  var zkClient: ZkClient = _

  def nodeNumber: Int

  def createNode = Cluster.node

  def barrier(name: String) = ZooKeeperBarrier(zkClient, ClusterName, name, "node-" + nodeNr, NrOfNodes)

  override def beforeAll() = {
    if (nodeNr == 1) zkServer = Cluster.startLocalCluster(DataPath, LogPath)
    zkClient = Cluster.newZkClient
  }

  override def beforeEach() = {
    //    if (nodeNr == 1) Cluster.reset
  }

  override def afterAll() = {
    zkClient.close
    if (nodeNr == 1) Cluster.shutdownLocalCluster
  }
}

class ClusterMultiJvmNode1 extends MultiNodeTest {
  def nodeNumber = 1

  "A cluster" should {

    "be able to start and stop - one node" in {

      Cluster setProperty ("akka.cluster.nodename" -> "node1")
      Cluster setProperty ("akka.cluster.port" -> "9991")
      import Cluster.node

      barrier("start-stop") {
        node.start()

        Thread.sleep(500)
        node.membershipNodes.size must be(1)

        //        node.stop()

        Thread.sleep(500)
        //        node.membershipNodes.size must be(0)
        //        node.isRunning must be(false)
      }
    }

    "be able to start and stop - two nodes" in {
      import Cluster.node

      barrier("start-node1") {
        node.start()
        Thread.sleep(500)
        node.membershipNodes.size must be(1)
      }

      barrier("start-node2") {
        // let node2 start
      }

      node.membershipNodes.size must be(2)
      node.leader must be(node.leaderLock.getId)

      barrier("stop-node1") {
        //        node.stop()
        Thread.sleep(500)
        //        node.isRunning must be(false)
      }

      barrier("stop-node2") {
        // let node2 stop
      }
    }
  }
}

class ClusterMultiJvmNode2 extends MultiNodeTest {
  def nodeNumber = 2

  "A cluster" should {

    "be able to start and stop - one node" in {
      Cluster setProperty ("akka.cluster.nodename" -> "node2")
      Cluster setProperty ("akka.cluster.port" -> "9992")

      barrier("start-stop") {
        // let node1 start
      }
    }

    "be able to start and stop - two nodes" in {
      import Cluster.node

      barrier("start-node1") {
        // let node1 start
      }

      barrier("start-node2") {
        node.start()
        Thread.sleep(500)
        node.membershipNodes.size must be(2)
      }

      barrier("stop-node1") {
        // let node1 stop
      }

      //      node.membershipNodes.size must be(1)
      //      node.leader must be(node.leaderLock.getId)

      barrier("stop-node2") {
        //        node.stop()
        //        Thread.sleep(500)
        //        node.isRunning must be(false)
      }
    }
  }
}