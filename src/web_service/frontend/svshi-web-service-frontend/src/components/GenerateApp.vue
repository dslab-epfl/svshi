<script lang="jsx">

let id = 1

export default {
    props: ['physStructAndSelectedDevices'],

    data() {
        return {
            appGenProto: {
                appName: "",
                priviledged: false,
                timer: "0",
                devices: []
            },
            defaultAppGenProto: {
                appName: "",
                priviledged: false,
                timer: "0",
                devices: []
            },
            physicalStructure: {},
            selectedDevices: [],
            successMessage: "Sucessful! Congratulations, your app is generated. It should be in your Download folder.\n\n\
Now, modify the code locally and then go to 'Install and simulate' to try it.",
        }
    },
    methods: {
        async generateNewAppAndDownloadFile() {
            let newAppName = this.appGenProto.appName;
            let newAppJson = {}
            newAppJson.permissionLevel = this.appGenProto.priviledged ? "privileged" : "notPrivileged"
            newAppJson.timer = parseInt(this.appGenProto.timer, 10)
            let devices = this.appGenProto.devices.map(d => this.deleteKeyFromObject(d, "id"))

            newAppJson.devices = devices

            console.log("sending generateApp request for: ")
            console.log(newAppJson)

            try {
                const requestOptions = {
                    method: "POST",
                    // headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(newAppJson, null, 4)
                };
                let res = await fetch(this.$root.backendServerAddressPort + "/generateApp/" + newAppName, requestOptions);
                if (res.status != 200) {
                    alert("An error occurred while generating the app...")
                } else {
                    this.downloadBytesAsFileBrowser(await res.blob(), newAppName + ".zip")
                    alert(this.successMessage)
                    this.$emit("main")
                }


            } catch (error) {
                alert("Could not generate the app, please check the values!")
                console.log(error);
            }

        },
        downloadBytesAsFileBrowser(bytes, filename) {
            let elm = document.createElement('a')
            elm.href = URL.createObjectURL(bytes)
            elm.setAttribute('download', filename) // SET ELEMENT CREATED 'ATTRIBUTE' TO DOWNLOAD, FILENAME PARAM AUTOMATICALLY
            elm.click()
        },
        back() {
            if (confirm("This will discard all changes. Are you sure you want to go back?")) {
                this.$emit("back")
            }
        },
        deleteKeyFromObject(obj, key) {
            return Object.fromEntries(Object.entries(obj).filter(el => el[0] !== key))
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
        isValidDeviceNameCharEvt(evt) {
            evt = (evt) ? evt : window.event;
            var charCode = (evt.which) ? evt.which : evt.keyCode;
            let valid = charCode == 95 || (charCode >= 65 && charCode <= 90) || (charCode >= 97 && charCode <= 122)
            if (!valid) {
                evt.preventDefault();;
            } else {
                return true;
            }
        },
        isValidDeviceNameChar(c) {
            var charCode = c.charCodeAt()
            return charCode == 95 || (charCode >= 65 && charCode <= 90) || (charCode >= 97 && charCode <= 122)

        },
        isValidAppNameCharEvt(evt) {
            evt = (evt) ? evt : window.event;
            var charCode = (evt.which) ? evt.which : evt.keyCode;
            let valid = charCode == 95 || (charCode >= 97 && charCode <= 122)
            if (!valid) {
                evt.preventDefault();;
            } else {
                return true;
            }
        },
        isValidAppNameChar(c) {
            var charCode = c.charCodeAt()
            return charCode == 95 || (charCode >= 97 && charCode <= 122)

        },
        replaceInvalidCharsByUnderscore(name) {
            let res = ""
            for (let i = 0; i < name.length; i++) {
                const c = name[i];
                if (this.isValidDeviceNameChar(c)) {
                    res += c
                } else {
                    res += "_"
                }
            }
            return res
        },
        addSelectedDevicesToProto: function (selectedDevices) {
            for (let index = 0; index < selectedDevices.length; index++) {
                const physDev = selectedDevices[index];
                for (let j = 0; j < physDev.supportedDeviceMappingNodes.length; j++) {
                    const node = physDev.supportedDeviceMappingNodes[j];
                    for (let k = 0; k < node.supportedDeviceMappings.length; k++) {
                        const dev = node.supportedDeviceMappings[k];
                        const newName = this.replaceInvalidCharsByUnderscore(dev.name)
                        this.appGenProto.devices.push({ id: dev.id, name: newName, deviceType: dev.supportedDeviceName, preBindingPhysId: dev.physicalCommObjectId })
                    }
                }
            }
        },
        getDeviceName(physAddr) {
            let opt = this.physicalStructure.deviceInstances.find(d => d.address == physAddr);
            if (typeof opt !== "undefined") {
                return opt.name;
            }
            else {
                return "";
            }
        },
        addIdsToSelectedDevices() {
            for (let i = 0; i < this.selectedDevices.length; i++) {
                const physDev = this.selectedDevices[i];
                for (let j = 0; j < physDev.supportedDeviceMappingNodes.length; j++) {
                    const node = physDev.supportedDeviceMappingNodes[j];
                    for (let k = 0; k < node.supportedDeviceMappings.length; k++) {
                        const dev = node.supportedDeviceMappings[k];
                        dev["id"] = id++

                    }
                }

            }
        },
        async refresh() {

        }
    },
    mounted() {
        id = 1
        this.physicalStructure = this.physStructAndSelectedDevices.physicalStructureJson
        this.selectedDevices = this.physStructAndSelectedDevices.selectedDevices
        this.addIdsToSelectedDevices()
        console.log("State at GenerateApp.vue")
        console.log(this.selectedDevices)
        this.addSelectedDevicesToProto(this.selectedDevices)
    }
}
</script>

<template>
    <div class="generateApp">
        <button class="classicButton backButton" @Click="this.back()">
            <FontAwesomeIcon icon="arrow-left" /> Back
        </button>
        <h2 class="generateApp">Generate new app</h2>
        <p class="generateApp">Now, please fill the information in the table. You can name your app, name your
            devices.</p>
        <p class="generateApp">If you check the privileged checkbox, your app will overwrite the behaviour of
            non-privileged app on a given system.</p>
        <p class="generateApp">The timer defines the maximal time between two executions (if
            no devices change state, SVSHI executes the app after "timer" seconds).</p>
        <div id="mainGridGenerateApp">
            <div id="mainGridColumn1">
                <template v-for="mapping in this.selectedDevices">
                    <div class="deviceMappingDiv">
                        <h3>{{ getDeviceName(mapping.physicalAddress) }} - {{ mapping.physicalAddress }}</h3>
                        <template v-for="node in mapping.supportedDeviceMappingNodes">
                            <div class="mappingNodeDiv">
                                <h4>{{ node.name }}</h4>
                                <template v-for="deviceMapping in node.supportedDeviceMappings">
                                    <div class="generateAppMappingProtoDeviceDiv">
                                        <p style="font-weight: bold">ID {{ deviceMapping.id }}</p>
                                        <p>{{ deviceMapping.name }}</p>
                                        <p class="supportedDeviceType"
                                            v-if="'humanReadableInfo' in deviceMapping && deviceMapping.humanReadableInfo !== ''">
                                            Type: {{ deviceMapping.supportedDeviceName }}
                                            ({{ deviceMapping.humanReadableInfo }})</p>
                                        <p class="supportedDeviceType" v-else>Type: {{
                                                deviceMapping.supportedDeviceName
                                        }}</p>

                                    </div>
                                </template>
                            </div>
                        </template>
                    </div>

                </template>
            </div>
            <div id="mainGridColumn2">
                <div id="appGrid">
                    <div class="appGridItem">App name:</div>
                    <div class="appGridItem"><input id="appGenAppNameTextField" type="text"
                            v-model="appGenProto.appName" @keypress="this.isValidAppNameCharEvt($event)">
                    </div>
                    <div class="appGridItem">Permission Level:</div>
                    <div class="appGridItem">
                        <div class="control-group">
                            <label class="control control-checkbox">
                                Privileged
                                <input id="appGenPrivilegedCheckbox" type="checkbox" v-model="appGenProto.privileged" />
                                <div class="control_indicator"></div>
                            </label>
                        </div>
                    </div>
                    <div class="appGridItem">Timer (0 for never):</div>
                    <div class="appGridItem"><input id="appGenTimerText" v-model="appGenProto.timer"
                            @keypress="isNumber($event)"></div>
                    <div class="appGridItem">Devices:</div>
                    <div class="appGridItem">
                        <template v-for='dev in this.appGenProto.devices' :key="dev.id">
                            <div id="appGridDeviceGrid">
                                <div class="appGridDeviceGridItem">
                                    <p style="font-weight: bold">{{ dev.id }}</p>
                                </div>
                                <div class="appGridDeviceGridItemName">
                                    <input id="appDeviceNameInput" v-model="dev.name" placeholder="device name"
                                        @keypress="this.isValidDeviceNameCharEvt($event)" />
                                </div>
                                <div class="appGridDeviceGridItemType">Type: {{ dev.deviceType }}</div>
                            </div>
                        </template>
                    </div>
                    <div class="appGridItem"></div>
                    <div class="appGridItem"></div>
                    <div class="appGridItem"></div>
                </div>
                <button class="classicButton generateButton"
                    @Click="this.generateNewAppAndDownloadFile()">Generate</button>
            </div>
        </div>

    </div>
</template>

<style>
@import '../assets/base.css';

.backButton {
    margin-top: 12px;
    margin-left: 12px;
}

#appDeviceNameInput {
    width: inherit
}

#appGrid {
    display: grid;
    column-gap: 18px;
    row-gap: 18px;
    grid-template-columns: 30% 70%;
    justify-content: space-evenly;
    margin: 28px;
    background-color: #f7f7f7;
    border-radius: 28px;
    padding: 45px;
}

#appGridDeviceGrid {
    display: grid;
    column-gap: 18px;
    grid-template-columns: 1% 50% 49%;
    background-color: #dddddd;
    padding: 8px;
    border-radius: 28px;
    width: 100%;
    margin-bottom: 4px;
    margin-top: 4px;
}

.appGridDeviceGridItemName {
    width: 250px;
}

.appGridDeviceGridItemType {
    width: fit-content;
}


#mainGridColumn1 {
    margin-right: 12px;
}

#mainGridColumn2 {
    margin-left: 12px;
}

#mainGridGenerateApp {
    display: grid;
    grid-template-columns: 35% 65%;
}

.generateAppMappingProtoDeviceDiv {
    background-color: #f7f7f7;
    border-radius: 28px;
    padding-left: 12px;
    padding-right: 12px;
    padding-top: 6px;
    padding-bottom: 6px;
    margin-top: 6px;
    margin-bottom: 6px;
}

h1.generateApp {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    text-align: center;
    margin-left: auto;
    margin-right: auto;
    margin-top: 8px;
    padding-top: 12px;
    padding-bottom: 12px;
    padding-left: 12px;
    padding-right: 12px;
    font-size: 2.2em;
}

p.generateApp {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    margin-left: auto;
    margin-right: auto;
    margin-top: 6px;
    text-align: left;
    padding-top: 6px;
    padding-bottom: 12px;
    font-size: 1.3em;
    width: fit-content;
}



h2.generateApp {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    text-align: left;
    margin-top: 8px;
    padding-top: 12px;
    padding-bottom: 12px;
    padding-left: 12px;
    padding-right: 12px;
    font-size: 2.2em;
    margin-bottom: 18px;
}

div.generateApp {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    margin-left: auto;
    margin-right: auto;
    margin-top: 20px;
    background-color: #e8c4a2;
    width: 90%;
    border-radius: 28px;
    padding-top: 12px;
    padding-bottom: 80px;
    padding-left: 12px;
    padding-right: 12px;
    height: fit-content;
}

table.newAppTable {
    background-color: #e8c4a2;
    margin-left: auto;
    margin-right: auto;
    width: 100%;
    padding-left: 30px;
    padding-right: 30px;
    padding-top: 32px;
    padding-bottom: 32px;
    font-family: Verdana;
    border-radius: 28px;
    margin-top: 12px;
}

td.newAppDevicesList {
    width: 100%;
}

td.newAppTable {
    padding: 12px;
}

.newAppDevices {
    padding-top: 22px;
    padding-bottom: 22px;
    padding-left: 10px;
    padding-right: 32px;
}

li.newAppDevicesList {
    list-style: none;
    margin-left: -42px;
    width: 100%;
}

button.generateButton {
    margin-top: 32px;
    margin-left: auto;
    margin-right: auto;
    width: 200px;
    height: 45px;
    display: block;
}
</style>
