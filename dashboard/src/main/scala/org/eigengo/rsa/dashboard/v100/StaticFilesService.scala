package org.eigengo.rsa.dashboard.v100

import akka.http.scaladsl.server.{Directives, Route}

trait StaticFilesService extends Directives {

  def staticFilesRoute: Route = getFromResourceDirectory("/src/main/webapp")

}
