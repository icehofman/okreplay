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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import software.betamax.Configuration
import software.betamax.Recorder
import spock.lang.*

import static com.google.common.net.HttpHeaders.VIA
import static software.betamax.TapeMode.READ_WRITE

@Issue("https://github.com/robfletcher/betamax/issues/16")
@Unroll
class IgnoreHostsSpec extends Specification {
  @Shared @AutoCleanup("deleteDir") File tapeRoot = Files.createTempDir()
  def configuration = Spy(Configuration, constructorArgs: [Configuration.builder()
      .tapeRoot(tapeRoot)
      .defaultMode(READ_WRITE)])
  def interceptor = new BetamaxInterceptor(configuration)
  @AutoCleanup("stop") Recorder recorder = new Recorder(configuration, interceptor)
  @Shared MockWebServer endpoint = new MockWebServer()

  def client = new OkHttpClient.Builder()
      .addInterceptor(interceptor)
      .build()

  void setupSpec() {
    endpoint.start()
  }

  void "does not proxy a request to #requestURI when ignoring #ignoreHosts"() {
    given: "proxy is configured to ignore local connections"
    configuration.getIgnoreHosts() >> [ignoreHosts]
    recorder.start("ignore hosts spec")

    when: "a request is made"
    endpoint.enqueue(new MockResponse().setBody("OK"))
    def request = new Request.Builder()
        .url(requestURI)
        .build()
    def response = client.newCall(request).execute()

    then: "the request is not intercepted by the proxy"
    println(configuration.getIgnoreHosts())
    response.header(VIA) == null

    and: "nothing is recorded to the tape"
    recorder.tape.size() == old(recorder.tape.size())

    where:
    ignoreHosts              | requestURI
    endpoint.url("/").host() | endpoint.url("/").toString()
    "localhost"              | "http://localhost:${endpoint.url("/").port()}"
    "127.0.0.1"              | "http://127.0.0.1:${endpoint.url("/").port()}"
    endpoint.url("/").host() | "http://localhost:${endpoint.url("/").port()}"
  }

  void "does not proxy a request to #requestURI when ignoreLocalhost is true"() {
    given: "proxy is configured to ignore local connections"
    configuration.ignoreLocalhost >> true
    recorder.start("ignore hosts spec")

    when: "a request is made"
    endpoint.enqueue(new MockResponse().setBody("OK"))
    def request = new Request.Builder()
        .url(requestURI)
        .build()
    def response = client.newCall(request).execute()

    then: "the request is not intercepted by the proxy"
    response.header(VIA) == null

    and: "nothing is recorded to the tape"
    recorder.tape.size() == old(recorder.tape.size())

    where:
    requestURI << [endpoint.url('/').toString(),
        "http://localhost:${endpoint.url('/').port()}"]
  }
}
