<script lang="js">
let id = 0

export default {

    data() {
        return {
            appGenProto: {
                appName: "",
                priviledged: false,
                timer: "0",
                files: "",
                devices: []
            },
            defaultAppGenProto: {
                appName: "",
                priviledged: false,
                timer: "0",
                files: "",
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
            newAppJson.files = this.appGenProto.files.split(";").filter(s => s !== "")
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
                    alert("'" + newAppName + "' was successfuly generated!")
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
        async refresh(){

        }
    },
    mounted() {
        this.loadAvailableDevices()
    }
}
</script>

<template>
    <h2>Generate a new app</h2>
    <table>
        <tr>
            <td>App name:</td>
            <input id="appGenAppNameTextField" type="text" v-model="appGenProto.appName" @keypress="this.isValidDeviceNameOrAppNameChar($event)">
        </tr>
        <tr>
            <td>Permission Level priviledged: </td>
            <td> <input id="appGenPriviledgedCheckbox" type="checkbox" v-model="appGenProto.priviledged"> </td>
        </tr>
        <tr>
            <td>Timer (0 for never): </td>
            <td><input id="appGenTimerText" v-model="appGenProto.timer" @keypress="isNumber($event)"></td>
        </tr>
        <tr>
            <td>Files (file names and extension, use ; to separate files): </td>
            <td><input id="appGenFilesText" v-model="appGenProto.files"></td>
        </tr>
        <tr>
            <td>Devices:</td>
            <td>
                <ul>
                    <li v-for='dev in this.appGenProto.devices' :key="dev.id">
                        <input v-model="dev.name" placeholder="device name" @keypress="this.isValidDeviceNameOrAppNameChar($event)" />
                        <select v-model="dev.deviceType">
                            <option disabled value="">Please select one</option>
                            <option v-for="deviceType in availableDevices" :value="deviceType">
                                {{ deviceType }}
                            </option>
                        </select>
                        <button
                            @Click="this.appGenProto.devices = this.appGenProto.devices.filter(d => d.id !== dev.id)">Remove</button>
                    </li>
                </ul>
                <button @Click="this.addNewDevice">Add</button>
            </td>
        </tr>
    </table>

    <button @Click="this.generateNewApp()">Generate</button>

</template>

<style>
@import '../assets/base.css';
</style>
