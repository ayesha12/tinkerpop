'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
'''
import requests
import json
from gremlin_driver import RemoteConnection
from gremlin_driver import Traverser

__author__ = 'Marko A. Rodriguez (http://markorodriguez.com)'


class RESTRemoteConnection(RemoteConnection):
    def __init__(self, url, scriptEngine):
        RemoteConnection.__init__(self, url,scriptEngine)

    def __repr__(self):
        return "RESTRemoteConnection[" + self.url + "]"

    def submit(self, script):
        response = requests.post(self.url, data=json.dumps({"gremlin": script})).json()
        results = []
        for x in response['result']['data']:
            results.append(Traverser(x, 1))
        return iter(results)