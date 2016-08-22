/*
 * The Reactive Summit Austin talk
 * Copyright (C) 2016 Jan Machacek
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.eigengo.rsa.dashboard.v100

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.trueaccord.scalapb.GeneratedMessage
import com.typesafe.config.{ConfigFactory, ConfigResolveOptions}

object Main extends App with DashboardService {
  Option(System.getenv("START_DELAY")).foreach(d ⇒ Thread.sleep(d.toInt))
  val config = ConfigFactory.load("dashboard.conf").resolve(ConfigResolveOptions.defaults())
  implicit val system = ActorSystem(name = "dashboard-100", config = config)
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  system.log.info(s"Dashboard 100 starting...")

  val route: Route = {
    if ("FALSE".equalsIgnoreCase(System.getenv("EMBEDDED_SERVER"))) {
      dashboardRoute
    } else {
      dashboardRoute ~ getFromResourceDirectory("")
    }
  }

  lazy val activeHandlesSource: Source[List[String], _] = Source.actorPublisher(ActiveHandlesActor.props)

  def eventsPerHandleSource(handle: String): Source[List[GeneratedMessage], _] = Source.actorPublisher(EventsPerHandleActor.props(handle))

  system.actorOf(DashboardSinkActor.props(config.getConfig("app")))
  Http(system).bindAndHandle(route, "0.0.0.0", 8080)
  system.log.info(s"Dashboard 100 running.")

}
