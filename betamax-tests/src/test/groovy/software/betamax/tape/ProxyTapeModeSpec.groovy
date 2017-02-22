/*
 * Copyright 2013 the original author or authors.
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

import okhttp3.Request
import software.betamax.Configuration
import software.betamax.tck.TapeModeSpec
import spock.lang.Ignore

@Ignore("https://github.com/adamfisk/LittleProxy/issues/113")
class ProxyTapeModeSpec extends TapeModeSpec {

  @Override
  protected Configuration getConfiguration() {
    Configuration.builder().tapeRoot(tapeRoot).build()
  }

  @Override
  protected void makeRequest() {
    def request = new Request.Builder()
        .url(endpoint.url("/"))
        .build()
    client.newCall(request).execute()
  }
}
