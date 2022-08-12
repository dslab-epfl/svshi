import { info } from 'console';
<script lang="jsx">
import { defineComponent } from 'vue'
import AppList from './AppList.vue'
import PulseLoader from '../../node_modules/vue-spinner/src/PulseLoader.vue'
import SvshiAppList from './SvshiAppList.vue'
import { Tabs, Tab } from 'vue3-tabs-component';

export default defineComponent({
    data() {
        return {
            selectedAppsForInstall: [],
            installInProgress: false,
            svshiCompileLogs: "",
            svshiSimulationRunning: false,
            logs: "",
            receivedTelegramsLogs: "",
            executionLogs: "",
            helpSimulatorText: "- to move a device: drag and drop with the left button of the mouse\n\
- to change state of a switch/push button: hold shift + left click\n\
- to change state of dimmer sensor: hold shift + hold left click on the device + move mouse up or down \n"
        };
    },
    computed: {},
    methods: {
        copyObject(obj) {
            return JSON.parse(JSON.stringify(obj))
        },
        back() {
            console.log("installSimulate emits back event");
            clearInterval(this.polling)
            this.polling = ""
            this.$emit("back");
        },
        onSelectedApps(selectedApps) {
            this.selectedAppsForInstall = this.copyObject(selectedApps)
            console.log("event: selectedAppsForInstall = " + JSON.stringify(this.selectedAppsForInstall))
        },
        async install() {
            if (this.selectedAppsForInstall.length === 0) {
                alert("Please select at least one app to install!")
                return
            }
            this.installInProgress = true
            try {
                const requestOptions = {
                    method: "POST",
                    // headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(this.selectedAppsForInstall, null, 4)
                };
                let res = await fetch(this.$root.backendServerAddressPort + "/svshi/applications/install/", requestOptions);
                if (res.status != 200) {
                    alert("An error occured while installing apps on svshi:\n" + await res.text())
                } else {
                    this.svshiCompileLogs = this.doubleLineBreaks(await res.text())
                }


            } catch (error) {
                alert("Could not install the apps!")
                console.log(error);
            }
            this.installInProgress = false
            this.refresh()
            this.$refs.appListComp.refresh()
        },
        doubleLineBreaks(s) {
            let res = ""
            for (let i = 0; i < s.length; i++) {
                const c = s[i];
                if (c === "\n") {
                    res += "\n"
                }
                res += c
            }
            return res
        },
        async startSimulation() {
            const requestOptions = {
                method: "POST",
            };
            let res = await fetch(this.$root.backendServerAddressPort + "/simulation/start", requestOptions);
            if (res.status != 200) {
                alert("An error occured while starting svshi simulation:\n" + await res.text())
            }
        },
        async stopSimulation() {
            const requestOptions = {
                method: "POST",
            };
            let res = await fetch(this.$root.backendServerAddressPort + "/simulation/stop", requestOptions);
            if (res.status != 200) {
                alert("An error occured while stopping svshi simulation:\n" + await res.text())
            }
        },
        showSimulatorHelp() {
            alert(this.helpSimulatorText)
        },
        // LOGS ------------------------------------------------------------------------------------------------
        async getRunStatus() {
            let res = await fetch(this.$root.backendServerAddressPort + "/svshi/runStatus/")
            let responseBody = await res.json()
            this.svshiSimulationRunning = responseBody.status
        },
        async getRunLogs() {
            try {
                let res = await fetch(this.$root.backendServerAddressPort + "/svshi/logs/run");
                if (res.status === 200) {
                    let responseBody = await res.json()
                    let logsLines = responseBody.lines
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
                let res = await fetch(this.$root.backendServerAddressPort + "/svshi/logs/receivedTelegrams");
                if (res.status === 200) {
                    let responseBody = await res.json()
                    let logsLines = responseBody.lines
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
                let res = await fetch(this.$root.backendServerAddress + "/svshi/logs/execution");
                if (res.status === 200) {
                    let responseBody = await res.json()
                    let logsLines = responseBody.lines
                    this.executionLogs = logsLines.join("\n\n")

                } else {
                    console.log("Cannot get execution logs!")
                }
            } catch (error) {
                console.log(error);
            }
        },
        // LOGS ------------------------------------------------------------------------------------------------
        refresh() {
            this.$refs.svshiAppListComp.refresh()
            this.getRunStatus()
            this.getRunLogs()
            this.getReceivedTelegramsLogs()
            this.getExecutionLogs()
        },
        startPolling() {
            this.polling = setInterval(() => {
                this.refresh()
            }, 1000)

        },
        async load() {
        }
    },
    mounted() {
        this.load();
        this.refresh();
        this.startPolling()
    },
    components: {
        AppList, PulseLoader, SvshiAppList, "tabs": Tabs,
        "tab": Tab,
    }
})
</script>

<template>
    <div class="installSimulate">
        <button class="classicButton backButton" @Click="this.back()">
            <FontAwesomeIcon icon="arrow-left" /> Back
        </button>
        <h1 class="installSimulate">Install and simulate</h1>
        <p class="installSimulate">Now that you developed some apps, you can install them on a SVSHI instance and get
            the verification output.</p>
        <p class="installSimulate">Once they are verified and installed, you can run SVSHI on a simulator and see how
            your apps would react in a KNX system and play with the devices.</p>
        <p class="installSimulate">Once you're happy with your app(s), you can download the files to install them on a
            local SVSHI installation.</p>
        <hr class="separator" />
        <p class="installSimulate">First, upload your new apps in the list below.</p>
        <p class="installSimulate">Please note that the app must have been generated using this service! You cannot use
            off the shelf SVSHI apps.</p>
        <h2>Available apps</h2>
        <div id="appList">
            <AppList v-if="!this.installInProgress" ref="appListComp" @onSelectApp="this.onSelectedApps" />
        </div>
        <p class="installSimulate">Now, select the one(s) you want to try on the simulator and click on "Install". The
            apps will be compiled by SVSHI and you'll get the output messages.</p>
        <div>
            <PulseLoader id="loader" v-if="this.installInProgress" :color="this.$root.colourOrangeSvshi" />
            <button id="installButton" v-if="!this.installInProgress" class="greenButton bigButton"
                @Click="this.install()">Install</button>
        </div>
        <textarea id="compileLogs" rows="30" v-model="this.svshiCompileLogs" readonly="true"></textarea>
        <hr class="separator" />
        <p class="installSimulate">Below you can see applications currently installed on SVSHI:</p>
        <h2>Apps on SVSHI</h2>
        <SvshiAppList ref="svshiAppListComp" />
        <h2>Simulate</h2>
        <p class="installSimulate">You can now simulate your apps on our KNX simulator. When clicking on "Simulate"
            below, it will open a simulator window and launch SVSHI with the apps installed on SVSHI.</p>
        <p class="installSimulate">To see the simulator, go to http://localhost:3001 for your browser.</p>
        <p class="installSimulate">Note: the simulator and SVSHI are ready when the line "INFO: Connecting to KNX and
            listening to
            telegrams..." appears in the logs below.</p>
        <button class="classicButton help" @Click="this.showSimulatorHelp()">Help simulator</button>
        <div>
            <button id="stopButton" v-if="!this.installInProgress && this.svshiSimulationRunning"
                class="redButton bigButton" @Click="this.stopSimulation()">Stop simulation</button>
            <button id="startButton" v-if="!this.installInProgress && !this.svshiSimulationRunning"
                class="greenButton bigButton" @Click="this.startSimulation()">Start simulation</button>
        </div>
        <div class="logs_div">
            <div class="logsTabs">
                <tabs panels-wrapper-class="tabs-component-panels-logs" wrapper-class="tabs-logs">
                    <tab name="Main logs">
                        <textarea rows="40" cols="100" v-model="this.logs" readonly="true"></textarea>
                    </tab>
                    <tab name="Received telegrams">
                        <textarea rows="40" cols="150" v-model="this.receivedTelegramsLogs" readonly="true"></textarea>
                    </tab>
                    <tab name="Debug logs">
                        <textarea rows="40" cols="150" v-model="this.executionLogs" readonly="true"></textarea>
                    </tab>
                    <!-- <tab name="Physical State">
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
                    </tab> -->
                </tabs>
            </div>
        </div>


    </div>
</template>

<style>
@import '../assets/base.css';

button.help {
    display: block;
    margin-left: auto;
    margin-right: 32px;
}

.tabs-logs {
    width: 65vw;
    display: block;
    margin-left: auto;
    margin-right: auto;
    margin-top: 60px;
}

.tabs-component-panels-logs {
    background-color: #e8c4a2;
    border: solid 1px #000;
    border-radius: 28px;
    box-shadow: 0 0 10px rgba(0, 0, 0, .05);
    padding: 4em 2em;
}

textarea {
    padding: 1em;
    border-color: #ccc;
    border-radius: 28px;
    font-size: 14px;
    background-color: #f4d4b5;
    width: 100%;
}

#appList {
    width: 100%;
    display: block;
    margin-left: auto;
    margin-right: auto;
}

#compileLogs {
    display: block;
    margin-left: auto;
    margin-right: auto;
    background-color: #f4d4b5;
    border-radius: 28px;
    padding: 1em;
    font-size: 1em;
    width: 98%;

}

hr.separator {
    width: 90%;
    height: 4px;
    border-radius: 28px;
    margin-left: auto;
    margin-right: auto;
    background-color: #666;
    border: 0 none;
    margin-top: 40px;
    margin-bottom: 40px;
}

button.bigButton {
    margin-top: 22px;
    margin-bottom: 22px;
    padding-left: 45px;
    padding-right: 45px;
    padding-top: 13px;
    padding-bottom: 13px;
    font-size: 2em;
    margin-left: auto;
    margin-right: auto;
    display: block;
}

td {
    text-align: center;
    vertical-align: middle;
}

.buttons {
    margin-left: auto;
    margin-right: auto;
}

p.installSimulate {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    margin-left: auto;
    margin-right: auto;
    margin-top: 6px;
    text-align: center;
    padding-top: 6px;
    padding-bottom: 12px;
    font-size: 1.3em;
}

h1.installSimulate {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    text-align: center;
    margin-left: auto;
    margin-right: auto;
    margin-top: 8px;
    padding-top: 12px;
    padding-bottom: 12px;
    padding-left: 12px;
    padding-right: 12px;
    font-size: 3em;
}

div.installSimulate {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    margin-left: auto;
    margin-right: auto;
    margin-top: 20px;
    background-color: #e8c4a2;
    width: 70%;
    border-radius: 28px;
    padding-top: 12px;
    padding-bottom: 80px;
    padding-left: 12px;
    padding-right: 12px;
    height: fit-content;
}


.tabs-component {
    margin: auto auto;
    width: 70vw;
    border-radius: 28px;
}

.tabs-component-tabs {
    border: solid 1px #000;
    border-radius: 6px;
    margin-bottom: 5px;
}

.tabs-component-tabs {
    border: 0;
    align-items: stretch;
    display: flex;
    justify-content: flex-start;
    margin-bottom: -1px;
}

.tabs-component-tab {
    color: rgb(79, 79, 79);
    font-size: 14px;
    font-weight: 600;
    margin-right: 0;
    list-style: none;
}

.tabs-component-tab:not(:last-child) {
    border-bottom: dotted 1px #000;
}

.tabs-component-tab:hover {
    color: #666;
}

.tabs-component-tab.is-active {
    color: #000;
}

.tabs-component-tab.is-disabled * {
    color: #cdcdcd;
    cursor: not-allowed !important;
}

.tabs-component-tab {
    background-color: #e8c4a2;
    border: solid 1px #000;
    border-radius: 28px 28px 0 0;
    margin-right: .5em;
    transform: translateY(2px);
    transition: transform .3s ease;
}

.tabs-component-tab.is-active {
    border-bottom: solid 1px #e8c4a2;
    z-index: 2;
    transform: translateY(0);
}


.tabs-component-tab-a {
    align-items: center;
    color: inherit;
    display: flex;
    padding: .75em 1em;
    text-decoration: none;
}

.tabs-component-panels {
    padding: 4em 0;
}

.tabs-component-panels {
    background-color: #e8c4a2;
    border: solid 1px #000;
    border-radius: 28px 28px 28px 28px;
    box-shadow: 0 0 10px rgba(0, 0, 0, .05);
    padding: 4em 2em;
}
</style>
