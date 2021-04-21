/*
 * This file is copied from Uber Remote Shuffle Service
(https://github.com/uber/RemoteShuffleService) and modified.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.shuffle.internal

import com.esotericsoftware.kryo.io.Output
import org.apache.spark.serializer.{SerializationStream, SerializerInstance}

import scala.collection.mutable

trait RecordSerializationBuffer[K, V] {

  def addRecord(partitionId: Int, record: Product2[K, V]): Seq[(Int, Array[Byte])]

  def filledBytes: Int

  def clear(): Seq[(Int, Array[Byte])]

  protected def serializeSortedPartitionedRecords[RK, RV](
                   sortedPartitionedIter: Iterator[((Int, RK), RV)],
                   serializerInstance: SerializerInstance,
                   bufferSize: Int):
      Seq[(Int, Array[Byte])] = {
    val result = mutable.Buffer[(Int, Array[Byte])]()

    val invalidPartitionId = Integer.MIN_VALUE
    var currentPartitionId = invalidPartitionId
    var currentPartitionRecords = 0
    // use 1g as max buffer size for Output to avoid KryoException: Buffer overflow
    val outputMaxBufferSize = 1024 * 1024 * 1024
    var output: Output = null
    var stream: SerializationStream = null
    while (sortedPartitionedIter.hasNext) {
      val item = sortedPartitionedIter.next()
      val partitionId = item._1._1
      val key: Any = item._1._2
      val value: Any = item._2
      if (partitionId != currentPartitionId) {
        if (stream != null) {
          stream.flush()
          result.append((currentPartitionId, output.toBytes))
          stream.close()
        }
        output = new Output(bufferSize, outputMaxBufferSize)
        stream = serializerInstance.serializeStream(output)
        currentPartitionRecords = 0
      }
      currentPartitionId = partitionId
      stream.writeKey(key)
      stream.writeValue(value)
      currentPartitionRecords += 1
    }

    if (currentPartitionRecords > 0) {
      stream.flush()
      result.append((currentPartitionId, output.toBytes))
      stream.close()
    }

    result
  }
}