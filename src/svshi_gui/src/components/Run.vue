<script lang="jsx">
import * as isIp from 'is-ip';
import { Tabs, Tab } from 'vue3-tabs-component';
let successStatusCode = 200
let notFoundStatusCode = 404


export default {
    components: {
        "tabs": Tabs,
        "tab": Tab,
    },
    data() {
        return {
            ipStr: "",
            portStr: "",
            isRunning: false,
            polling: null,
            logs: "",
            receivedTelegramsLogs: "",
            executionLogs: "",
            currentPhysicalState: {}
        }
    },
    methods: {
        isNumberDot(evt) {
            evt = (evt) ? evt : window.event;
            var charCode = (evt.which) ? evt.which : evt.keyCode;
            if ((charCode > 31 && (charCode < 48 || charCode > 57)) && charCode !== 46) {
                evt.preventDefault();;
            } else {
                return true;
            }
        },
        isNumber(evt) {
            evt = (evt) ? evt : window.event;
            var charCode = (evt.which) ? evt.which : evt.keyCode;
            if ((charCode > 31 && (charCode < 48 || charCode > 57)) && charCode !== 46) {
                evt.preventDefault();;
            } else {
                return true;
            }
        },
        isValidIp(ipStr) {
            return isIp.isIPv4(ipStr)
        },
        isValidPort(portStr) {
            return !isNaN(parseInt(portStr, 10))
        },
        async downloadAssignments() {
            let url = this.$root.backendServerAddress + "/assignments"
            let filename = "assignments.zip"
            try {
                let response = await fetch(url, {
                    method: 'GET',
                    headers: {
                        'Accept': 'application/zip'
                    }
                })
                let status = await response.status
                if (status === successStatusCode) {
                    let bytes = await response.blob()

                    let elm = document.createElement('a')
                    elm.href = URL.createObjectURL(bytes)
                    elm.setAttribute('download', filename) // SET ELEMENT CREATED 'ATTRIBUTE' TO DOWNLOAD, FILENAME PARAM AUTOMATICALLY
                    elm.click()

                } else if (status === notFoundStatusCode) {
                    alert("Assignments are not created yet. Did you compile the apps?")
                } else {
                    alert("An error occured while downloading assignments!")
                }
            }
            catch (error) {
                console.log(error)
            }
        },
        async run() {
            if (this.isValidIp(this.ipStr) && this.isValidPort(this.portStr)) {
                const requestOptions = {
                    method: "POST",
                    // headers: { "Content-Type": "application/json" },
                    body: ""
                }
                let res = await fetch(this.$root.backendServerAddress + "/run/" + this.ipStr + ":" + this.portStr, requestOptions)
                this.isRunning = true
                let responseBody = await res.json()
                if (!responseBody.status) {
                    var message = "An error occurred while launching svshi run!"
                    alert(message)
                }
            } else {
                alert("Please enter a valid IP and port combination!")
            }
        },
        async killSvshi() {
            const requestOptions = {
                method: "POST",
                // headers: { "Content-Type": "application/json" },
                body: ""
            }
            let res = await fetch(this.$root.backendServerAddress + "/stopRun", requestOptions)
            let responseBody = await res.json()
            if (!responseBody.status) {
                var message = "An error occurred while stopping svshi run!"
                alert(message)
            }
        },
        async getRunStatus() {
            let res = await fetch(this.$root.backendServerAddress + "/runStatus/")
            let responseBody = await res.json()
            this.isRunning = responseBody.status
        },
        async getRunLogs() {
            try {
                let res = await fetch(this.$root.backendServerAddress + "/logs/run");
                if (res.status === successStatusCode) {
                    let responseBody = await res.json()
                    let logsLines = responseBody.output
                    this.logs = logsLines.join("\n")

                } else {
                    console.log("Cannot get run logs!")
                }
            } catch (error) {
                console.log(error);
            }
        },
        async getReceivedTelegramsLogs() {
            try {
                let res = await fetch(this.$root.backendServerAddress + "/logs/receivedTelegrams");
                if (res.status === successStatusCode) {
                    let responseBody = await res.json()
                    let logsLines = responseBody.output
                    this.receivedTelegramsLogs = logsLines.join("\n\n")

                } else {
                    console.log("Cannot get received telegrams logs!")
                }
            } catch (error) {
                console.log(error);
            }
        },
        async getExecutionLogs() {
            try {
                let res = await fetch(this.$root.backendServerAddress + "/logs/execution");
                if (res.status === successStatusCode) {
                    let responseBody = await res.json()
                    let logsLines = responseBody.output
                    this.executionLogs = logsLines.join("\n\n")

                } else {
                    console.log("Cannot get execution logs!")
                }
            } catch (error) {
                console.log(error);
            }
        },
        async getPhysicalStateLog() {
            try {
                let res = await fetch(this.$root.backendServerAddress + "/logs/physicalState");
                if (res.status === successStatusCode) {
                    let physicalState = await res.json()
                    this.currentPhysicalState = physicalState
                } else {
                    console.log("Cannot get physicalState log!")
                }
            } catch (error) {
                console.log(error);
            }
        },
        prettyPrintWithDpt(value, dpt) {
            if(value === null){
                return "None"
            }
            let dptMainNumber = parseInt(dpt.replace("DPT", ""))
            if (dptMainNumber === 1) {
                if (value === true || value === false) {
                    if (value) {
                        return "On"
                    } else {
                        return "Off"
                    }
                }
            }

            return value.toString() // Default behaviour: the value in a string
        },
        async refresh() {
            this.getRunStatus()
            if (this.isRunning) {
                this.getRunLogs()
                this.getReceivedTelegramsLogs()
                this.getExecutionLogs()
                this.getPhysicalStateLog()
            }
        },
        pollData() {
            this.polling = setInterval(() => {
                this.refresh()
            }, 1000)
        }
    },
    beforeDestroy() {
        clearInterval(this.polling)
    },
    mounted() {
        this.pollData()
    }

}
</script>

<template>
    <h2>Run</h2>
    <div v-if="this.isRunning" class="box_running">
        <strong>Running</strong>
    </div>
    <div v-if="!this.isRunning" class="box_stopped">
        <strong>Stopped</strong>
    </div>
    <p>Please enter the IP and port of your KNX interface:</p>
    <input id="ipInput" v-model="this.ipStr" @keypress="isNumberDot($event)" placeholder="Ip address e.g. 127.0.0.1" />
    <input id="portInput" v-model="this.portStr" @keypress="isNumber($event)" placeholder="Port e.g. 3671" />
    <button class="greenButton" @Click="this.run" v-if="!this.isRunning">Run</button>
    <button class="redButton" @Click="this.killSvshi" v-if="this.isRunning">Stop</button>

    <button class="classicButton" @Click="this.downloadAssignments" v-if="!this.isRunning">Download assignments</button>

    <div class="logs_div">
        <h3>Logs</h3>
        <tabs>
            <tab name="Main logs">
                <textarea rows="40" cols="100" v-model="this.logs" readonly="true"></textarea>
            </tab>
            <tab name="Received telegrams">
                <textarea rows="40" cols="150" v-model="this.receivedTelegramsLogs" readonly="true"></textarea>
            </tab>
            <tab name="Debug logs">
                <textarea rows="40" cols="150" v-model="this.executionLogs" readonly="true"></textarea>
            </tab>
            <tab name="Physical State">
                <ul style="list-style-type:none;">
                    <li v-for='ga in Object.keys(this.currentPhysicalState)'>
                        <table>
                            <tr>
                                <td>{{ ga }}</td>
                                <td>
                                    <p> ==> </p>
                                </td>
                                <td>{{ this.prettyPrintWithDpt(this.currentPhysicalState[ga].value,
                                        this.currentPhysicalState[ga].dpt)
                                }}</td>
                            </tr>
                        </table>
                    </li>
                </ul>
            </tab>
        </tabs>
    </div>

</template>

<style>
body {
    font: normal 1em/1 sans-serif;
}

textarea {
    padding-left: 2px;
    padding-top: 2px;
    border-color: #ccc;
    font-size: 14px;
}

.box_running {
    align-items: center;
    background-color: #2db051;
    color: white;
    display: flex;
    justify-content: center;
    height: 110px;
    position: relative;
    width: 460px;
}

.box_running:before {
    content: attr(data-caption);
    font-size: 18px;
    left: 20px;
    position: absolute;
    text-transform: uppercase;
    top: 20px;
}

.box_running strong {
    font-size: 80px;
}

.box_stopped {
    align-items: center;
    background-color: #dd2323;
    color: white;
    display: flex;
    justify-content: center;
    height: 110px;
    position: relative;
    width: 460px;
}

.box_stopped:before {
    content: attr(data-caption);
    font-size: 18px;
    left: 20px;
    position: absolute;
    text-transform: uppercase;
    top: 20px;
}

.box_stopped strong {
    font-size: 80px;
}
</style>
