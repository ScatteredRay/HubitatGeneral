/**
 *  WeatherUndergroundCustom
 *
 *  Copyright 2018 mattw01
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
    definition (name: "WeatherUndergroundCustom", namespace: "mattw01", author: "mattw01") {
        capability "Temperature Measurement"
        capability "Illuminance Measurement"
        capability "Relative Humidity Measurement"
        command "poll"
        command "forcePoll"
    }
    preferences() {
        section("Query Inputs"){
            input "apiKey", "text", required: true, title: "API Key"
            input "pollLocation", "text", required: true, title: "ZIP Code or Location"
            input "pollInterval", "number", required: true, title: "Poll Interval"
            input "autoPoll", "bool", required: false, title: "Automatically poll"
        }
    }
}

def updated() {
    log.debug "updated called"
    log.debug "now: ${now()}"
    unschedule()
    forcePoll()
    if(autoPoll)
    runIn(pollInterval * 60, pollSchedule)
}
def pollSchedule()
{
    forcePoll()
    runIn(pollInterval * 60, pollSchedule)
}

def parse(String description) {
}

def poll()
{
    if(now() - state.lastPoll > (pollInterval * 60000))
        forcePoll()
    else
        log.debug "poll called before interval threshold was reached"
}

def forcePoll()
{
    log.debug "WU: forcePoll called"
    def params = [
        uri: "http://api.wunderground.com/api/${apiKey}/conditions/q/${pollLocation}.json"
    ]
    log.debug "params: ${params}"
    try {
        httpGet(params) { resp ->
            resp.headers.each {
                log.debug "Response: ${it.name} : ${it.value}"
            }
            log.debug "response contentType: ${resp.contentType}"
            log.debug "response data: ${resp.data}"
            sendEvent(name: "temperature", value: resp.data.current_observation.temp_f, unit: "F")
            sendEvent(name: "solarradiation", value: resp.data.current_observation.solarradiation, unit: "W")
            sendEvent(name: "humidity", value: resp.data.current_observation.relative_humidity, unit: "%")
            // Map solarradiation to illuminance for now
            sendEvent(name: "illuminance", value: resp.data.current_observation.solarradiation, unit: "lux")
            
            state.lastPoll = now()
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
    
}