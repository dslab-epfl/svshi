<script lang="jsx">
let id = 0

export default {

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
            availableDevices: ["notLoadedYet"]
        }
    },
    methods: {
        async loadAvailableDevices() {
            try {
                let res = await fetch(this.$root.backendServerAddress + "/availableProtoDevices");
                let responseBody = await res.json();
                this.availableDevices = responseBody.output
            } catch (error) {
                console.log(error);
            }
        },
        async generateNewApp() {
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
                let res = await fetch(this.$root.backendServerAddress + "/generateApp/" + newAppName, requestOptions);
                let responseBody = await res.json();
                if (responseBody.status) {
                    alert("'" + newAppName + "' was successfuly generated! Please download the files from the 'new apps' list and write your code.")
                } else {
                    var message = "An error occurred while generating '" + newAppName + "'! Please see the following logs: "
                    let array = responseBody.output
                    array.forEach(element => {
                        message += "\n"
                        message += element
                    });
                    alert(message)
                }
                this.appGenProto = this.defaultAppGenProto

            } catch (error) {
                alert("Could not generate the app, please check the values!")
                console.log(error);
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
        isValidDeviceNameOrAppNameChar(evt) {
            evt = (evt) ? evt : window.event;
            var charCode = (evt.which) ? evt.which : evt.keyCode;
            let valid = charCode == 95 || (charCode >= 65 && charCode <= 90) || (charCode >= 97 && charCode <= 122)
            if (!valid) {
                evt.preventDefault();;
            } else {
                return true;
            }
        },
        addNewDevice: function () {
            this.appGenProto.devices.push({ id: id++, name: "", deviceType: "" })
        },
        async refresh() {

        }
    },
    mounted() {
        this.loadAvailableDevices()
    }
}
</script>

<template>
    <h2>Generate new app</h2>
    <table class="newAppTable">
        <tr>
            <td class="newAppTable">App name:</td>
            <td class="newAppTable">
                <input id="appGenAppNameTextField" type="text" v-model="appGenProto.appName"
                    @keypress="this.isValidDeviceNameOrAppNameChar($event)">
            </td>
        </tr>
        <tr>
            <td class="newAppTable">Permission Level: </td>
            <td class="newAppTable">
                <div class="control-group">
                    <label class="control control-checkbox">
                        Priviledged
                        <input id="appGenPriviledgedCheckbox" type="checkbox" v-model="appGenProto.priviledged" />
                        <div class="control_indicator"></div>
                    </label>
                </div>
            </td>
        </tr>
        <tr>
            <td class="newAppTable">Timer (0 for never): </td>
            <td class="newAppTable"><input id="appGenTimerText" v-model="appGenProto.timer"
                    @keypress="isNumber($event)"></td>
        </tr>
        <tr class="newAppDevices">
            <td class="newAppDevices">Devices:</td>
            <td class="newAppDevicesList">
                <ul>
                    <li class="newAppDevicesList" v-for='dev in this.appGenProto.devices' :key="dev.id">
                        <table>
                            <tr>
                                <td>
                                    <input v-model="dev.name" placeholder="device name"
                                        @keypress="this.isValidDeviceNameOrAppNameChar($event)" />
                                </td>
                                <td>
                                    <div class="selectSvshi">
                                        <select v-model="dev.deviceType">
                                            <option disabled value="">Please select one</option>
                                            <option v-for="deviceType in availableDevices" :value="deviceType">
                                                {{ deviceType }}
                                            </option>
                                        </select>
                                        <div class="selectSvshi_arrow">
                                        </div>
                                    </div>
                                </td>
                                <td>
                                    <button class="redButton"
                                        @Click="this.appGenProto.devices = this.appGenProto.devices.filter(d => d.id !== dev.id)">
                                        <FontAwesomeIcon icon="trash-can" />
                                    </button>
                                </td>
                            </tr>
                        </table>
                    </li>
                </ul>
                <button class="greenButton" @Click="this.addNewDevice">
                    <FontAwesomeIcon icon="plus" />
                </button>
            </td>
        </tr>
    </table>

    <button class="classicButton generateButton" @Click="this.generateNewApp()">Generate</button>

</template>

<style>
@import '../assets/base.css';

table.newAppTable {
    background-color: #e8c4a2;
    margin-left: auto;
    margin-right: auto;
    width: 60%;
    padding-left: 30px;
    padding-right: 30px;
    padding-top: 32px;
    padding-bottom: 32px;
    font-family: Verdana;
    border-radius: 28px;
    margin-top: 12px;
}

td.newAppDevicesList {
    width: 70%;
}

td.newAppTable {
    padding: 10px;
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
}

.control {
    font-family: arial;
    display: block;
    position: relative;
    padding-left: 30px;
    margin-bottom: 5px;
    padding-top: 3px;
    cursor: pointer;
    font-size: 16px;
}

.control input {
    position: absolute;
    z-index: -1;
    opacity: 0;
}

.control_indicator {
    position: absolute;
    top: 2px;
    left: 0;
    height: 20px;
    width: 20px;
    background: #e6e6e6;
    border: 0px solid #000000;
    border-radius: 7px;
}

.control:hover input~.control_indicator,
.control input:focus~.control_indicator {
    background: #e4e4e4;
}

.control input:checked~.control_indicator {
    background: #e87000;
}

.control:hover input:not([disabled]):checked~.control_indicator,
.control input:checked:focus~.control_indicator {
    background: #c66000;
}

.control input:disabled~.control_indicator {
    background: #e6e6e6;
    opacity: 0.6;
    pointer-events: none;
}

.control_indicator:after {
    box-sizing: unset;
    content: '';
    position: absolute;
    display: none;
}

.control input:checked~.control_indicator:after {
    display: block;
}

.control-checkbox .control_indicator:after {
    left: 8px;
    top: 4px;
    width: 3px;
    height: 8px;
    border: solid #ffffff;
    border-width: 0 2px 2px 0;
    transform: rotate(45deg);
}

.control-checkbox input:disabled~.control_indicator:after {
    border-color: #7b7b7b;
}

.control-checkbox .control_indicator::before {
    content: '';
    display: block;
    position: absolute;
    left: 0;
    top: 0;
    width: 4.5rem;
    height: 4.5rem;
    margin-left: -1.3rem;
    margin-top: -1.3rem;
    background: #2aa1c0;
    border-radius: 3rem;
    opacity: 0.6;
    z-index: 99999;
    transform: scale(0);
}
</style>
