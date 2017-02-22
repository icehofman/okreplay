/*
 * Copyright 2011 the original author or authors.
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

package software.betamax.proxy

import com.google.common.io.Files
import org.junit.ClassRule
import software.betamax.Configuration
import software.betamax.junit.Betamax
import software.betamax.junit.RecorderRule
import software.betamax.util.server.OkHandler
import software.betamax.util.server.SimpleServer
import spock.lang.*

import static com.google.common.net.HttpHeaders.VIA
import static java.net.HttpURLConnection.HTTP_OK
import static software.betamax.TapeMode.READ_WRITE

@Betamax(mode = READ_WRITE)
@Unroll
@Timeout(10)
class RequestMethodsSpec extends Specification {

  @Shared @AutoCleanup("deleteDir") def tapeRoot = Files.createTempDir()
  @Shared def configuration = Configuration.builder().tapeRoot(tapeRoot).build()
  @Shared @ClassRule RecorderRule recorder = new RecorderRule(configuration)

  @Shared @AutoCleanup("stop") def endpoint = new SimpleServer(OkHandler)

  void setupSpec() {
    endpoint.start()
  }

  void "proxy handles #method requests"() {
    when:
    HttpURLConnection connection = endpoint.url.toURL().openConnection()
    connection.requestMethod = method

    then:
    connection.responseCode == HTTP_OK
    connection.getHeaderField(VIA) == "Betamax"

    where:
    method << ["GET", "POST", "PUT", "HEAD", "DELETE", "OPTIONS"]
  }

}
