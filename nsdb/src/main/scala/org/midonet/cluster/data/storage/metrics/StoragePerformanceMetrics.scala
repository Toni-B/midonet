/*
 * Copyright 2016 Midokura SARL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.midonet.cluster.data.storage.metrics

import java.util.concurrent.TimeUnit.NANOSECONDS

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.MetricRegistry.name

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.CuratorEventType
import org.apache.curator.framework.api.CuratorEventType._
import org.apache.curator.framework.state.ConnectionState._
import org.apache.curator.framework.state.{ConnectionState, ConnectionStateListener}

import org.midonet.cluster.monitoring.metrics.{StorageHistogram, StorageMeter, StorageTimer}

class StoragePerformanceMetrics(registry: MetricRegistry) {

    final def addLatency(eventType: CuratorEventType, latencyInNanos: Long)
    : Unit = eventType match {
        case CREATE | DELETE | SET_DATA =>
            addWriteLatency(latencyInNanos)
        case EXISTS | GET_DATA =>
            addReadLatency(latencyInNanos)
        case CHILDREN =>
            addReadChildrenLatency(latencyInNanos)
        case _ =>
    }

    private val connectionsLostMeter =
        registry.meter(name(classOf[StorageMeter], "connectionsLost"))
    private val connectionsCreatedMeter =
        registry.meter(name(classOf[StorageMeter], "connectionsCreated"))

    private val readTimer =
        registry.timer(name(classOf[StorageTimer], "read"))
    private val readChildrenTimer =
        registry.timer(name(classOf[StorageTimer], "readChildren"))
    private val writeTimer =
        registry.timer(name(classOf[StorageTimer], "write"))
    private val multiTimer =
        registry.timer(name(classOf[StorageTimer], "multi"))

    private val stateTableReadLatency =
        registry.histogram(name(classOf[StorageHistogram], "stateTable",
                                "readLatency"))
    private val stateTableAddLatency =
        registry.histogram(name(classOf[StorageHistogram], "stateTable",
                                "addLatency"))
    private val stateTableRoundTripLatency =
        registry.histogram(name(classOf[StorageHistogram], "stateTable",
                                "roundTripLatency"))

    def addReadLatency(latencyInNanos: Long): Unit =
        readTimer.update(latencyInNanos, NANOSECONDS)

    def addReadChildrenLatency(latencyInNanos: Long): Unit =
        readChildrenTimer.update(latencyInNanos, NANOSECONDS)

    def addWriteLatency(latencyInNanos: Long): Unit =
        writeTimer.update(latencyInNanos, NANOSECONDS)

    def addMultiLatency(latencyInNanos: Long): Unit =
        multiTimer.update(latencyInNanos, NANOSECONDS)

    def addStateTableReadLatency(latencyInNanos: Long): Unit =
        stateTableReadLatency.update(latencyInNanos)

    def addStateTableAddLatency(latencyInNanos: Long): Unit =
        stateTableAddLatency.update(latencyInNanos)

    def addStateTableRoundTripLatency(latencyInNanos: Long): Unit =
        stateTableRoundTripLatency.update(latencyInNanos)

    def connectionStateListener(): ConnectionStateListener = {
        new ConnectionStateListener {
            override def stateChanged(client: CuratorFramework,
                                      state: ConnectionState): Unit =
                state match {
                    case CONNECTED => connectionsCreatedMeter.mark()
                    case LOST => connectionsLostMeter.mark()
                    case _ =>
                }
        }
    }
}
