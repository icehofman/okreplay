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

package software.betamax.tape

import com.google.common.io.Files
import okhttp3.*
import org.yaml.snakeyaml.Yaml
import software.betamax.tape.yaml.YamlTapeLoader
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import static com.google.common.net.HttpHeaders.*
import static com.google.common.net.MediaType.FORM_DATA
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST
import static java.net.HttpURLConnection.HTTP_OK
import static software.betamax.TapeMode.READ_WRITE

class WriteTapeToYamlSpec extends Specification {

  @Shared @AutoCleanup("deleteDir") def tapeRoot = Files.createTempDir()
  @Shared def loader = new YamlTapeLoader(tapeRoot)

  @Shared Request getRequest
  @Shared Request postRequest
  @Shared Response successResponse
  @Shared Response failureResponse
  @Shared Response imageResponse
  @Shared File image

  Yaml yamlReader

  void setupSpec() {
    getRequest = new Request.Builder()
        .url("http://freeside.co/betamax")
        .addHeader(ACCEPT_LANGUAGE, "en-GB,en")
        .addHeader(IF_NONE_MATCH, "b00b135")
        .build()

    postRequest = new Request.Builder()
        .method("POST", RequestBody.create(MediaType.parse(FORM_DATA.toString()), "q=1"))
        .url("http://github.com/")
        .build()

    successResponse = new Response.Builder()
        .code(HTTP_OK)
        .body(ResponseBody.create(MediaType.parse("text/plain"), "O HAI!"))
        .addHeader(CONTENT_LANGUAGE, "en-GB")
        .addHeader(CONTENT_ENCODING, "none")
        .build()

    failureResponse = new Response.Builder()
        .code(HTTP_BAD_REQUEST)
        .addHeader(CONTENT_LANGUAGE, "en-GB")
        .addHeader(CONTENT_ENCODING, "none")
        .body(ResponseBody.create(MediaType.parse("text/plain"), "KTHXBYE!"))
        .build()

    image = new File(Class.getResource("/image.png").toURI())
    imageResponse = new Response.Builder()
        .code(HTTP_OK)
        .body(ResponseBody.create(MediaType.parse("image/png"), image.bytes))
        .build()
  }

  void setup() {
    yamlReader = new Yaml()
  }

  void "can write a tape to storage"() {
    given:
    def tape = loader.newTape("tape_loading_spec")
    tape.mode = READ_WRITE
    def writer = new StringWriter()

    when:
    tape.record(getRequest, successResponse)
    loader.writeTo(tape, writer)

    then:
    def yaml = yamlReader.loadAs(writer.toString(), Map)
    yaml.size() == 2
    yaml.name == tape.name

    yaml.interactions.size() == 1
    yaml.interactions[0].recorded instanceof Date
    yaml.interactions[0].request.method == "GET"
    yaml.interactions[0].request.uri == "http://freeside.co/betamax"
    yaml.interactions[0].response.status == HTTP_OK
    yaml.interactions[0].response.body == "O HAI!"
  }

  void "writes request headers"() {
    given:
    def tape = loader.newTape("tape_loading_spec")
    tape.mode = READ_WRITE
    def writer = new StringWriter()

    when:
    tape.record(getRequest, successResponse)
    loader.writeTo(tape, writer)

    then:
    def yaml = yamlReader.loadAs(writer.toString(), Map)
    yaml.interactions[0].request.headers[ACCEPT_LANGUAGE] == "en-GB,en"
    yaml.interactions[0].request.headers[IF_NONE_MATCH] == "b00b135"
  }

  void "writes response headers"() {
    given:
    def tape = loader.newTape("tape_loading_spec")
    tape.mode = READ_WRITE
    def writer = new StringWriter()

    when:
    tape.record(getRequest, successResponse)
    loader.writeTo(tape, writer)

    then:
    def yaml = yamlReader.loadAs(writer.toString(), Map)
    yaml.interactions[0].response.headers[CONTENT_TYPE] == "text/plain"
    yaml.interactions[0].response.headers[CONTENT_LANGUAGE] == "en-GB"
    yaml.interactions[0].response.headers[CONTENT_ENCODING] == "none"
  }

  void "can write requests with a body"() {
    given:
    def tape = loader.newTape("tape_loading_spec")
    tape.mode = READ_WRITE
    def writer = new StringWriter()

    when:
    tape.record(postRequest, successResponse)
    loader.writeTo(tape, writer)

    then:
    def yaml = yamlReader.loadAs(writer.toString(), Map)
    yaml.interactions[0].request.method == "POST"
    yaml.interactions[0].request.body == "q=1"
  }

  void "can write multiple interactions"() {
    given:
    def tape = loader.newTape("tape_loading_spec")
    tape.mode = READ_WRITE
    def writer = new StringWriter()

    when:
    tape.record(getRequest, successResponse)
    tape.record(postRequest, failureResponse)
    loader.writeTo(tape, writer)

    then:
    def yaml = yamlReader.loadAs(writer.toString(), Map)
    yaml.interactions.size() == 2
    yaml.interactions[0].request.method == "GET"
    yaml.interactions[1].request.method == "POST"
    yaml.interactions[0].response.status == HTTP_OK
    yaml.interactions[1].response.status == HTTP_BAD_REQUEST
  }

  void "can write a binary response body"() {
    given:
    def tape = loader.newTape("tape_loading_spec")
    tape.mode = READ_WRITE
    def writer = new StringWriter()

    when:
    tape.record(getRequest, imageResponse)
    loader.writeTo(tape, writer)

    then:
    def yaml = yamlReader.loadAs(writer.toString(), Map)
    yaml.interactions[0].response.headers[CONTENT_TYPE] == "image/png"
    yaml.interactions[0].response.body == image.bytes
  }

  void "text response body is written to file as plain text"() {
    given:
    def tape = loader.newTape("tape_loading_spec")
    tape.mode = READ_WRITE
    def writer = new StringWriter()

    when:
    tape.record(getRequest, successResponse)
    loader.writeTo(tape, writer)

    then:
    writer.toString().contains("body: O HAI!")
  }

  void "binary response body is written to file as binary data"() {
    given:
    def tape = loader.newTape("tape_loading_spec")
    tape.mode = READ_WRITE
    def writer = new StringWriter()

    when:
    tape.record(getRequest, imageResponse)
    loader.writeTo(tape, writer)

    then:
    writer.toString().contains("body: !!binary |-")
  }

}
