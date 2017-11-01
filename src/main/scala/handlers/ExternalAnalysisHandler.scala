/*
 * Copyright 2016-2017 original author or authors
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

package handlers

import java.io.File
import javax.inject.Inject

import tap.pipelines.materialize.PipelineContext.{executor, materializer}
import com.typesafe.config.ConfigFactory
import models.Results.{StringListResult, StringResult}
import play.api.{Configuration, Environment, Logger, Mode}
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfig, AhcWSClientConfigFactory}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
/**
  * Created by andrew@andrewresearch.net on 19/9/17.
  */
class ExternalAnalysisHandler @Inject() (wsClient: WSClient) {

  val logger: Logger = Logger(this.getClass)

  def analyseWithAthanor(text:String,grammar:Option[String]):Future[StringListResult] = {
    //logger.info(s"Analysing with athanor: $text")
    val parameter = "?grammar="+grammar.getOrElse("analytic")
    val url = "http://athanor.utscic.edu.au/v2/analyse/text/rhetorical"+parameter
    logger.info(s"Creating request to: $url")
    val request: WSRequest = wsClient.url(url)

    val athanorRequest: WSRequest = request
        .withHttpHeaders("Accept" -> "application/json")
        .withRequestTimeout(10000.millis)

    val futureResponse: Future[WSResponse] = athanorRequest.post(text)

    case class AthanorMsg(message:String, results:List[List[String]])

    import play.api.libs.functional.syntax._  //scalastyle:ignore
    import play.api.libs.json._               //scalastyle:ignore

    implicit val AMWrites: Writes[AthanorMsg] = (
      (JsPath \ "message").write[String] and
        (JsPath \ "results").write[List[List[String]]]
    )(unlift(AthanorMsg.unapply))

    implicit val AMReads:Reads[AthanorMsg] = (
      (JsPath \ "message").read[String] and
        (JsPath \ "results").read[List[List[String]]]
      )(AthanorMsg.apply _)

    val result:Future[List[List[String]]] = futureResponse.map { response =>
      response.json.as[AthanorMsg].results
    }
    
    result.map(s => StringListResult(s))
  }

//  def analyseWithXip(text:String):Future[StringResult] = Future {
//
//    StringResult("")
//  }

}
