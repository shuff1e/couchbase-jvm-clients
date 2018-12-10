/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.client.scala

import java.time.Duration
import java.util.Optional

import com.couchbase.client.core.error.{CouchbaseException, DocumentDoesNotExistException}
import com.couchbase.client.core.msg.ResponseStatus
import com.couchbase.client.core.msg.kv._
import com.couchbase.client.core.retry.BestEffortRetryStrategy
import com.couchbase.client.core.util.Validators

import scala.compat.java8.FunctionConverters._
import com.couchbase.client.scala.api._
import com.couchbase.client.scala.document.{JsonObject, ReadResult}
import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.buffer.ByteBuf
import reactor.core.scala.publisher.Mono
import rx.RxReactiveStreams

import scala.compat.java8.FutureConverters
import scala.concurrent.{ExecutionContext, Future, JavaConversions}
import scala.concurrent.duration.{FiniteDuration, _}



class AsyncCollection(val collection: Collection) {
  private val core = collection.scope.core
  private val mapper = new ObjectMapper()
  private var coreContext = null
  private val kvTimeout = collection.kvTimeout

  private def encode[T](content: T): Array[Byte] = null
  implicit def scalaDurationToJava(in: scala.concurrent.duration.FiniteDuration): java.time.Duration = {
    java.time.Duration.ofNanos(in.toNanos)
  }

  def insert[T](id: String,
                content: T,
                timeout: FiniteDuration = kvTimeout,
                expiration: FiniteDuration = 0.seconds,
                replicateTo: ReplicateTo.Value = ReplicateTo.None,
                persistTo: PersistTo.Value = PersistTo.None
            )(implicit ec: ExecutionContext): Future[MutationResult] = {
    Validators.notNullOrEmpty(id, "id")
    Validators.notNull(content, "content")

    // TODO custom encoders
    val encoded = encode(content)
    // TODO is expiration in nanos? (mn: no, expiration is in seconds) ;-)
    // TODO flags
    // TODO datatype
    val retryStrategy = BestEffortRetryStrategy.INSTANCE // todo fixme from env
    val bucketName = "BUCKETNAME" // todo fixme from constructor chain
    val collection = null
    val request = new InsertRequest(id, collection, encoded, expiration.toSeconds, 0, timeout, coreContext, bucketName, retryStrategy)
    core.send(request)
    FutureConverters.toScala(request.response())
      .map(v => {
        // TODO
        //        if (response.status().isSuccess) {
        //          val out = JSON_OBJECT_TRANSCODER.newDocument(doc.id(), doc.expiry(), doc.content(), response.cas(), response.mutationToken())
        //          out
        //        }
        //        // TODO move this to core
        //        else response.status() match {
        //          case ResponseStatus.TOO_BIG =>
        //            throw addDetails(new RequestTooBigException, response)
        //          case ResponseStatus.EXISTS =>
        //            throw addDetails(new DocumentAlreadyExistsException, response)
        //          case ResponseStatus.TEMPORARY_FAILURE | ResponseStatus.SERVER_BUSY =>
        //            throw addDetails(new TemporaryFailureException, response)
        //          case ResponseStatus.OUT_OF_MEMORY =>
        //            throw addDetails(new CouchbaseOutOfMemoryException, response)
        //          case _ =>
        //            throw addDetails(new CouchbaseException(response.status.toString), response)
        //        }

        MutationResult(0, None)
      })
  }

  def insert[T](id: String,
             content: T,
             options: InsertOptions
            )(implicit ec: ExecutionContext): Future[MutationResult] = {
    Validators.notNull(options, "options")
    insert(id, content, options.timeout, options.expiration, options.replicateTo, options.persistTo)
  }

  def replace[T](id: String,
                 content: T,
                 cas: Long,
                 timeout: FiniteDuration = kvTimeout,
                 expiration: FiniteDuration = 0.seconds,
                 replicateTo: ReplicateTo.Value = ReplicateTo.None,
                 persistTo: PersistTo.Value = PersistTo.None
             )(implicit ec: ExecutionContext): Future[MutationResult] = {
    Validators.notNullOrEmpty(id, "id")
    Validators.notNull(content, "content")
    Validators.notNull(cas, "cas")

    // TODO custom encoders
    val encoded = encode(content)
    // TODO is expiration in nanos? (mn: no, expiration is in seconds) ;-)
    // TODO flags
    // TODO datatype
    val retryStrategy = BestEffortRetryStrategy.INSTANCE // todo fixme from env
    val bucketName = "BUCKETNAME" // todo fixme from constructor chain
    val collection = null

    val request = new ReplaceRequest(id, null, encoded, expiration.toSeconds, 0, timeout, cas, coreContext, bucketName, retryStrategy)
    core.send(request)
    FutureConverters.toScala(request.response())
      .map(v => {
        // TODO
        MutationResult(0, None)
      })
  }

  def replace[T](id: String,
              content: T,
              cas: Long,
              options: ReplaceOptions
             )(implicit ec: ExecutionContext): Future[MutationResult] = {
    replace(id, content, cas, options.timeout, options.expiration, options.replicateTo, options.persistTo)
  }

  def upsert(id: String,
             content: JsonObject,
             cas: Long,
             timeout: FiniteDuration = kvTimeout,
             expiration: FiniteDuration = 0.seconds,
             replicateTo: ReplicateTo.Value = ReplicateTo.None,
             persistTo: PersistTo.Value = PersistTo.None
             )(implicit ec: ExecutionContext): Future[MutationResult] = {
    Validators.notNullOrEmpty(id, "id")
    Validators.notNull(content, "content")
    Validators.notNull(cas, "cas")

    // TODO custom encoders
    val encoded = encode(content)
    // TODO is expiration in nanos? (mn: no, expiration is in seconds) ;-)
    // TODO flags
    // TODO datatype
    val retryStrategy = BestEffortRetryStrategy.INSTANCE // todo fixme from env
    val bucketName = "BUCKETNAME" // todo fixme from constructor chain
    val collection = null

    val request = new UpsertRequest(id, collection, encoded, expiration.toSeconds, 0, timeout, coreContext, bucketName, retryStrategy)
    core.send(request)
    FutureConverters.toScala(request.response())
      .map(v => {
        // TODO
        MutationResult(0, None)
      })
  }

  def upsert(id: String,
              content: JsonObject,
              cas: Long,
              options: UpsertOptions
             )(implicit ec: ExecutionContext): Future[MutationResult] = {
    upsert(id, content, cas, options.timeout, options.expiration, options.replicateTo, options.persistTo)
  }


  def remove(id: String,
             cas: Long,
             timeout: FiniteDuration = kvTimeout,
             replicateTo: ReplicateTo.Value = ReplicateTo.None,
             persistTo: PersistTo.Value = PersistTo.None
            )(implicit ec: ExecutionContext): Future[MutationResult] = {
    Validators.notNullOrEmpty(id, "id")
    Validators.notNull(cas, "cas")

    val retryStrategy = BestEffortRetryStrategy.INSTANCE // todo fixme from env
    val bucketName = "BUCKETNAME" // todo fixme from constructor chain
    val collection = null

    val request = new RemoveRequest(id, collection, cas, timeout, coreContext, bucketName, retryStrategy)
    core.send(request)
    FutureConverters.toScala(request.response())
      .map(v => {
        // TODO
        //        if (response.status().isSuccess) {
        //          val out = RemoveResult(response.cas(), Option(response.mutationToken()))
        //          out
        //        }
        //        // TODO move this to core
        //        else response.status() match {
        //          case ResponseStatus.NOT_EXISTS =>
        //            throw addDetails(new DocumentDoesNotExistException, response)
        //          case ResponseStatus.EXISTS | ResponseStatus.LOCKED =>
        //            throw addDetails(new CASMismatchException, response)
        //          case ResponseStatus.TEMPORARY_FAILURE | ResponseStatus.SERVER_BUSY =>
        //            throw addDetails(new TemporaryFailureException, response)
        //          case ResponseStatus.OUT_OF_MEMORY =>
        //            throw addDetails(new CouchbaseOutOfMemoryException, response)
        //          case _ =>
        //            throw addDetails(new CouchbaseException(response.status.toString), response)
        //        }


        MutationResult(0, None)
      })
  }

  def remove(id: String,
             cas: Long,
             options: RemoveOptions
            )(implicit ec: ExecutionContext): Future[MutationResult] = {
    remove(id, cas, options.timeout)
  }

  def lookupInAs[T](id: String,
                    operations: ReadSpec,
                    timeout: FiniteDuration = kvTimeout)
                   (implicit ec: ExecutionContext): Future[T] = {
    return null;
  }

  def get(id: String,
          timeout: FiniteDuration = kvTimeout)
         (implicit ec: ExecutionContext): Future[Option[ReadResult]] = {

    Validators.notNullOrEmpty(id, "id")
    Validators.notNull(timeout, "timeout")

    val retryStrategy = BestEffortRetryStrategy.INSTANCE // todo fixme from env
    val bucketName = "BUCKETNAME" // todo fixme from constructor chain
    val collection = null

    val request = new GetRequest(id,collection, timeout, coreContext, bucketName, retryStrategy)
    core.send(request)
    FutureConverters.toScala(request.response())
      .map(v => {
        if (v.status() == ResponseStatus.NOT_FOUND) {
          None
        }
        else if (v.status() != ResponseStatus.SUCCESS) {
          // TODO
          throw new CouchbaseException()
        }
        else {
          // TODO
//          val content = JsonObject.create()
//          Some(new GetResult(id, v.cas(), v.content()))
          null
        }
      })

//    val request = new GetRequest(id, Duration.ofNanos(timeout.toNanos), coreContext)
//
//    dispatch[GetRequest, GetResponse](request)
//      .map(response => {
//        if (response.status().success()) {
//          val content = mapper.readValue(response.content(), classOf[JsonObject])
////          val doc = JSON_OBJECT_TRANSCODER.decode(id, response.content(), response.cas(), 0, response.flags(), response.status())
//          val doc = JsonDocument.create(id, content, response.cas())
//          Option(doc)
//        }
//        // TODO move this to core
//        else response.status match {
//          case ResponseStatus.NOT_FOUND =>
//            Option.empty[JsonDocument]
//          case _ =>
//            throw addDetails(new CouchbaseException(response.status.toString), response)
//        }
//      })
  }

  def get(id: String,
          options: ReadOptions
         )(implicit ec: ExecutionContext): Future[Option[ReadResult]] = {
    get(id, options.timeout)
  }

  def getOrError(id: String,
                 timeout: FiniteDuration = kvTimeout)
                (implicit ec: ExecutionContext): Future[ReadResult] = {
    get(id, timeout).map(doc => {
      if (doc.isEmpty) throw new DocumentDoesNotExistException()
      else doc.get
    })
  }

  def getOrError(id: String,
                 options: ReadOptions)
                (implicit ec: ExecutionContext): Future[ReadResult] = {
    getOrError(id, options.timeout)
  }

  def getAndLock(id: String,
                 lockFor: FiniteDuration,
                 timeout: FiniteDuration = kvTimeout)
                (implicit ec: ExecutionContext) = Future {
    Option.empty
  }

  def getAndLock(id: String,
                 lockFor: FiniteDuration,
                 options: GetAndLockOptions)
                (implicit ec: ExecutionContext) = Future {
    Option.empty
  }



}
