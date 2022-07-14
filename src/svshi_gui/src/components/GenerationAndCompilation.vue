<script lang="jsx">
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'
import Bindings from './Bindings.vue'
import JSZip from 'jszip'
import InstalledAppList from './InstalledAppList.vue';
import NewAppList from './NewAppList.vue';
import JsonViewer from 'vue-json-viewer'
import vSelect from "vue-select";
import path from 'path';
import { compile } from 'vue';

let successStatusCode = 200

export default {
    components: {
        PulseLoader,
        Bindings,
        InstalledAppList,
        JsonViewer,
        NewAppList,
        "v-select": vSelect
    },
    data() {
        return {
            knxProjFile: '',
            jsonPhysFile: '',
            compileGenerateBindingsLoading: false,
            updateLoading: false,
            newApps: [],
            appToUpdate: "",
            installedApps: [],
            updateButtonDisabled: false,
            simulatorMode: false,
            zipFileUploadNewApps: '',
            stepWelcome: 0,
            stepPhysSystem: 1,
            stepBindingsNewApps: 2,
            stepBindingsExistingApps: 3,
            stepLogs: 4,
            installationStep: 0,
            appBindings: [],
            physicalStructure: {},
            filteredPhysicalStructure: {},
            searchPhysicalStructure: {},
            searchField: "",
            physicalPhysIds: [],
            logs: "",
            selectedComObjectFilling: ""
        }
    },
    computed: {
        newAppsBindings: function () {
            return this.appBindings.filter(a => this.newApps.map(a => a.name).includes(a.name))
        },
        existingAppsBindings: function () {
            return this.appBindings.filter(a => !this.newApps.map(a => a.name).includes(a.name))
        }
    },
    methods: {
        updateFileKnxProj(evt) {
            if (this.$refs.knxProjFile.files.length > 0) {
                let file = this.$refs.knxProjFile.files[0]
                console.log(file)
                if (!file.name.endsWith(".knxproj")) {
                    alert("Please upload a .knxproj file!")
                    this.$refs.knxProjFile.value = null
                } else {
                    this.knxProjFile = file
                }
            } else {
                this.knxProjFile = ''
            }
        },
        updateFileJson(evt) {
            if (this.$refs.jsonPhysFile.files.length > 0) {
                let file = this.$refs.jsonPhysFile.files[0]
                console.log(file)
                if (!file.name.endsWith(".json")) {
                    alert("Please upload a .json file!")
                    this.$refs.jsonPhysFile.value = null
                } else {
                    this.jsonPhysFile = file
                }
            } else {
                this.jsonPhysFile = ''
            }
        },
        async isSvshiRunning() {
            try {
                let res = await fetch(this.$root.backendServerAddress + "/runStatus")
                let responseBody = await res.json()
                this.isRunning = responseBody.status
                return this.isRunning
            }
            catch (error) {
                console.log(error)
                return false
            }
        },
        async compile() {
            if (await this.isSvshiRunning()) {
                alert("You cannot compile while SVSHI is running!")
                return
            }
            if (this.bindingsReady()) {
                await this.uploadNewBindings()
                let success = await this.executeAction("compile")
                if (success) {
                    this.appBindings = []
                }
                this.installationStep = this.stepLogs
            } else {
                alert("Please fill the bindings below before compiling!")
            }
        },
        async generateBindings() {
            if (await this.isSvshiRunning()) {
                alert("You cannot generateBindings while SVSHI is running!")
                return
            }
            this.appBindings = []
            await this.deleteInGenerated("appBindings.json", false)
            await this.executeAction("generateBindings")

            this.goToNewAppsBindings()

        },
        async executeAction(action) {
            if (action === "compile" || action === "generateBindings") {
                if (this.simulatorMode && this.jsonPhysFile === '') {
                    alert("Please upload a .json file for the physical structure before!")
                    return
                }
                if (!this.simulatorMode && this.knxProjFile === '') {
                    alert("Please upload a .knxproj file before!")
                    return
                }
                try {
                    this.logs = ""
                    let file = this.simulatorMode ? this.jsonPhysFile : this.knxProjFile
                    let zip = JSZip()
                    zip.file(file.name, file)
                    let zipFile = new Blob([await zip.generateAsync({ type: "uint8array" })])
                    this.compileGenerateBindingsLoading = true
                    let formData = new FormData()
                    let url = action === "compile" ? this.$root.backendServerAddress + "/compile" : this.$root.backendServerAddress + "/generateBindings"
                    formData.append('method', 'POST')
                    formData.append('icon', zipFile)
                    let response = await fetch(url, {
                        method: 'POST',
                        headers: {
                            'Accept': 'application/json',
                            'Content-Type': 'multipart/form-data'
                        },
                        body: formData
                    })

                    this.$refs.jsonPhysFile.value = null
                    this.$refs.knxProjFile.value = null

                    let responseBody = await response.json();
                    var message = ""
                    if (responseBody.status) {
                        if (action === "compile") {
                            message += "Compilation was successful!"
                        } else {
                            message += "Bindings were successfully generated!"
                        }
                        this.refresh()
                    } else {
                        if (action === "compile") {
                            message += "An error occurred while compiling! Please see the following logs:"
                        } else {
                            message += "An error occurred while generating bindings! Please see the following logs:"
                        }
                    }
                    let array = responseBody.output
                    array.forEach(element => {
                        message += "\n"
                        message += element
                    })
                    this.compileGenerateBindingsLoading = false
                    this.logs = message
                    return responseBody.status
                } catch (error) {
                    console.log(error)
                    this.compileGenerateBindingsLoading = false
                }
            }
        },
        async updateApp() {
            if (this.updatable()) {
                try {
                    let appToUpdateLocal = this.appToUpdate
                    this.updateLoading = true
                    let url = this.$root.backendServerAddress + "/updateApp/" + appToUpdateLocal
                    let response = await fetch(url, {
                        method: 'POST',
                        headers: {
                            'Accept': 'application/json'
                        },
                        body: ""
                    })
                    let responseBody = await response.json();
                    var message = ""
                    if (responseBody.status) {
                        message += "Update of '" + appToUpdateLocal + "' was successful!"
                        this.refresh()
                    } else {
                        message += "An error occurred while updating '" + appToUpdateLocal + "'! Please see the following logs:"
                    }
                    let array = responseBody.output
                    array.forEach(element => {
                        message += "\n"
                        message += element
                    })
                    this.updateLoading = false
                    if (!responseBody.status) {
                        alert(message)
                        return false
                    } else {
                        console.log(message)
                        return true
                    }
                } catch (error) {
                    console.log(error)
                    this.updateLoading = false
                }
            }
        },
        async deleteInGenerated(filename, confirmation = true) {
            if (!confirmation || confirm("This cannot be undone! Are you sure you want to delete '" + filename + "'?")) {
                let response = await fetch(this.$root.backendServerAddress + "/deleteGenerated/" + filename, {
                    method: 'POST',
                    headers: {
                        'Accept': 'application/json'
                    }
                })
                let responseBody = await response.json();
                if (!responseBody.status) {
                    var message = "An error occurred while deleting the requested file/directory! Please see the following logs: "
                    let array = responseBody.output
                    array.forEach(element => {
                        message += "\n"
                        message += element
                    });
                    alert(message)
                }
            }
        },
        async downloadFile(url, filename) {
            try {
                console.log("Download '" + filename + "' from '" + url + "'")
                let response = await fetch(url, {
                    method: 'GET',
                    headers: {
                        'Accept': 'application/zip'
                    }
                })
                let bytes = await response.blob()

                let elm = document.createElement('a')
                elm.href = URL.createObjectURL(bytes)
                elm.setAttribute('download', filename) // SET ELEMENT CREATED 'ATTRIBUTE' TO DOWNLOAD, FILENAME PARAM AUTOMATICALLY
                elm.click()
            }
            catch (error) {
                console.log(error)
            }
        },
        async getInstalledApps() {
            try {
                let res = await fetch(this.$root.backendServerAddress + "/listApps");
                let responseBody = await res.json();
                let array = responseBody.output
                let currentId = 0
                let ret = []
                array.forEach(element => {
                    ret.push({ id: currentId++, name: element })
                });
                return ret
            } catch (error) {
                console.log(error);
                return []
            }
        },
        updatable() {
            return this.newApps.length === 1 && this.newApps.map(a => a.name).includes(this.appToUpdate) && this.installedApps.map(a => a.name).includes(this.appToUpdate)
        },
        copyObject(obj) {
            return JSON.parse(JSON.stringify(obj))
        },
        finishInstallationProcess() {
            this.appBindings = []
            this.installationStep = this.stepWelcome
        },
        async getNewApps() {
            try {
                let res = await fetch(this.$root.backendServerAddress + "/newApps");
                let responseBody = await res.json();
                let array = responseBody.output
                let currentId = 0
                let ret = []
                array.forEach(element => {
                    ret.push({ id: currentId++, name: element })
                });
                return ret
            } catch (error) {
                console.log(error);
                return []
            }
        },

        // Bindings part --------------------------------------------------------------------------------------------------------
        bindingsReady() {
            let usedIds = this.appBindings.flatMap(app => app.bindings.map(b => b.binding.physDeviceId))
            return !usedIds.includes(-1) && !usedIds.some(i => !this.physicalPhysIds.includes(i))
        },
        filterPhysicalStructure: function () {
            if (Object.keys(this.physicalStructure).length > 0) {
                this.filteredPhysicalStructure.deviceInstances = this.physicalStructure.deviceInstances.filter(d => {
                    let filteredNodes = d.nodes.filter(n => n.comObjects.length > 0)
                    return filteredNodes.length > 0
                })
                this.physicalPhysIds = this.filteredPhysicalStructure.deviceInstances.flatMap(d => d.nodes.flatMap(n => n.comObjects.map(c => c.id))).sort()
            }
        },
        async getBindings() {
            try {
                let res = await fetch(this.$root.backendServerAddress + "/bindings");
                if (res.status === successStatusCode) {
                    let responseBody = await res.json();
                    this.appBindings = responseBody.bindings.appBindings
                    this.physicalStructure = responseBody.physicalStructure

                } else {
                    // console.log("Bindings not generated!")
                    this.appBindings = []
                    this.physicalStructure = {}
                }
            } catch (error) {
                console.log(error);
                this.appBindings = []
                this.physicalStructure = {}
            }
            this.filterPhysicalStructure()
        },
        isIntPosNeg(evt) {
            evt = (evt) ? evt : window.event;
            var charCode = (evt.which) ? evt.which : evt.keyCode;
            if ((charCode > 31 && (charCode < 48 || charCode > 57)) && charCode !== 45) {
                evt.preventDefault();;
            } else {
                return true;
            }
        },
        async uploadNewBindings() {
            let jsonBindings = {
                appBindings: this.appBindings
            }
            let fileToUpload = await this.createArchive(JSON.stringify(jsonBindings, null, 4))
            let formData = new FormData()
            formData.append('method', 'POST')
            formData.append('icon', fileToUpload)
            let response = await fetch(this.$root.backendServerAddress + "/generated", {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'multipart/form-data'
                },
                body: formData
            })
            let responseBody = await response.json();
            if (responseBody.status) {
                console.log("New bindings files were uploaded successfuly!")
            } else {
                var message = "An error occurred while uploading '" + this.zipFileToUpload.name + "'! Please see the following logs: "
                let array = responseBody.output
                array.forEach(element => {
                    message += "\n"
                    message += element
                });
                console.log(message)
            }
        },
        async createArchive(jsonContent) {
            var zip = new JSZip();

            var content = jsonContent
            zip.file("apps_bindings.json", content);

            let result = await zip.generateAsync({ type: "uint8array" });
            return new Blob([result])
        },
        updateSearchPhysicalStructure() {
            if (this.searchField === "") {
                this.searchPhysicalStructure = {}
            } else {
                this.searchPhysicalStructure = {}
                this.searchPhysicalStructure.deviceInstances = this.physicalStructure.deviceInstances.map(d => {
                    return {
                        "name": d.name,
                        "address": d.address,
                        "nodes": d.nodes.map(n => {
                            return {
                                "name": n.name,
                                "comObjects": n.comObjects.filter(c => c.name.toLowerCase().includes(this.searchField.toLowerCase()))
                            }

                        }).filter(n => n.comObjects.length > 0)
                    }
                }).filter(d => d.nodes.length > 0)
            }
        },
        async goToNewAppsBindings() {
            this.appBindings = []
            await this.getBindings()
            let bindingsNewAppsHaveDevices = this.newAppsBindings.map(b => b.bindings.length).some(x => x > 0)
            if (this.newApps.length > 0 && bindingsNewAppsHaveDevices) {
                this.installationStep = this.stepBindingsNewApps
            } else {
                this.goToExistingAppsBindingsOrCompile()
            }
        },
        goToExistingAppsBindingsOrCompile() {
            this.installationStep = this.stepBindingsExistingApps
            let bindingsExistingAppsHaveDevices = this.existingAppsBindings.map(b => b.bindings.length).some(x => x > 0)
            console.log(this.newApps)
            if (this.existingAppsBindings.length === 0 || !bindingsExistingAppsHaveDevices) {
                this.compile()
            }
        },
        onSearchFieldChange(evt) {
            this.searchField = evt.target.value
            this.updateSearchPhysicalStructure()
        },
        onKeyClick(v) {
            if (this.selectedComObjectFilling !== "") {
                console.log(v)
                let id = this.getIdFromKeyPathStringInPhysicalStruct(v)
                console.log(this.getIdFromKeyPathStringInPhysicalStruct(v))
                this.selectedComObjectFilling.binding.physDeviceId = id
            }

        },
        getIdFromKeyPathStringInPhysicalStruct(s) {
            let path = s.split(".")
            if (path.shift() !== "$") {
                return -1
            }
            let physStruct = Object.keys(this.searchPhysicalStructure).length === 0 ? this.filteredPhysicalStructure : this.searchPhysicalStructure
            for (let i = 0; i < path.length; i++) {
                let elm = path[i]
                let n = Number(elm)
                if (Number.isInteger(n)) {
                    physStruct = physStruct[n]
                } else {
                    physStruct = physStruct[elm]
                }
                if (typeof physStruct === 'undefined') {
                    return -1
                }
                if (elm === "id") {
                    return physStruct
                }
            }
            return -1
        },
        cancelInstallation(){
            this.installationStep = this.stepWelcome
        },
        async refresh() {
            this.updateButtonDisabled = !this.updatable()
            this.installedApps = await this.getInstalledApps()
            this.newApps = await this.getNewApps()
            if (this.appBindings.length === 0) {
                await this.getBindings()
            }
            this.$refs.installedAppListComp.refresh()
            if (!this.uninstalling) {
                this.$refs.newAppListComp.refresh()
            }
        }
    },
    mounted() {
        this.refresh()
    }
}
</script>

<template>
    <InstalledAppList class="installedAppsComp" ref="installedAppListComp" />
    <NewAppList class="newAppListComp" ref="newAppListComp" />

    <div class="installationStep" v-if="this.installationStep === stepWelcome">
        <h2>Install new apps</h2>
        <button class="classicButton" v-if="this.newApps.length > 0"
            @Click="this.installationStep = stepPhysSystem">Start installation</button>
        <button class="classicButton" v-if="this.newApps.length === 0 && this.installedApps.length > 0"
            @Click="this.installationStep = stepPhysSystem">Reassign bindings</button>
        <p v-if="this.newApps.length === 0 && this.installedApps.length === 0">No app to install!</p>
    </div>
    <div class="installationStep" v-if="this.installationStep === stepPhysSystem">
        <h2>Physical System (step 1/4)</h2>
        <div class="uploadPhysDiv">
            <p class="physicalStructureUploadInstructions">Here, please upload a file representing your KNX system's
                devices. This will be used by the system to detect
                devices present in your infrastructure and usable by SVSHI.
                You can upload either a `.knxproj` file as exported by ETS or, by activating the "simulator mode", a
                `.json`
                file that can be generated in the "Physical System simulator" tab.
            </p>
            <div class="control-group">
                <label class="control control-checkbox">
                    Simulator mode
                    <input id="appGenPriviledgedCheckbox" type="checkbox" v-model="simulatorMode" />
                    <div class="control_indicator"></div>
                </label>
            </div>
            <div v-if="simulatorMode">
                <p class="warning">Use this mode only when using the simulator. When using a real physical system, use
                    the
                    .knxproj file input.</p>
                <table>
                    <tr>
                        <td>
                            <p>Select a JSON file for the physical structure: </p>
                        </td>
                        <td>
                            <input type="file" id="file" ref="jsonPhysFile" class="custom-file-input"
                                @change="updateFileJson">
                        </td>
                    </tr>
                </table>
            </div>
            <div v-else>
                <table>
                    <tr>
                        <td>
                            <p>Please upload a KNX ETS project file (extension .knxproj):</p>
                        </td>
                        <td>
                            <input type="file" id="file" ref="knxProjFile" class="custom-file-input"
                                @change="updateFileKnxProj">
                        </td>
                    </tr>
                </table>


            </div>
        </div>
        <button class="redButton" @Click="this.cancelInstallation" v-if="!compileGenerateBindingsLoading">Cancel</button>
        <button class="classicButton" @Click="generateBindings" v-if="!compileGenerateBindingsLoading">Next</button>
        <div>
            <PulseLoader :color="this.$root.colourOrangeSvshi" v-if="compileGenerateBindingsLoading" />
        </div>
    </div>
    <div class="installationStep" v-if="this.installationStep === this.stepBindingsNewApps">
        <h2>Devices Bindings - new apps (step 2/4)</h2>
        <p>Now, please bind devices from new applications to devices of the physical structure by assigning them the
            right id which you can find on the physical device on the right.<br />You can select the device and then
            click on the "id" word of the wanted id on the physical structure to bind it.</p>
        <table class="bindingsTable" v-if="this.newAppsBindings.length > 0">
            <tr>
                <td>Bindings</td>
                <td>Physical structure</td>
            </tr>
            <tr>
                <td></td>
                <td>
                    <input :value="this.searchField" placeholder="Search com objects"
                        @input="this.onSearchFieldChange($event)" />
                </td>
            </tr>
            <tr>
                <td valign="top">
                    <ul>
                        <li v-for='app in newAppsBindings' :key="app.name">
                            <p class="appNameBindings">{{ app.name }}</p>
                            <ul>
                                <li v-for='b in app.bindings' :key="b.name">
                                    <div class="activeComObject comObject" @Click="this.selectedComObjectFilling = ''"
                                        v-if="this.selectedComObjectFilling === b">
                                        <p>Name: {{ b.name }} <br>
                                            Type: {{ b.binding.typeString }} <br>
                                            PhysicalID:
                                        </p>
                                        <v-select class="selectPhysId" :options="this.physicalPhysIds"
                                            :searchable="true" :filterable="true" v-model="b.binding.physDeviceId"
                                            :no-drop="false"></v-select>
                                    </div>
                                    <div class="inactiveComObject comObject" @Click="this.selectedComObjectFilling = b"
                                        v-else>
                                        <p>Name: {{ b.name }} <br>
                                            Type: {{ b.binding.typeString }} <br>
                                            PhysicalID:
                                        </p>
                                        <v-select class="selectPhysId" :options="this.physicalPhysIds"
                                            :searchable="true" :filterable="true" v-model="b.binding.physDeviceId"
                                            :no-drop="false"></v-select>
                                    </div>
                                </li>
                            </ul>
                        </li>
                    </ul>
                </td>
                <td>
                    <JsonViewer theme="my-awesome-json-theme" @keyclick="this.onKeyClick"
                        v-if="Object.keys(this.searchPhysicalStructure).length === 0" :value="filteredPhysicalStructure"
                        :expand-depth=3></JsonViewer>
                    <JsonViewer theme="my-awesome-json-theme" @keyclick="this.onKeyClick"
                        v-if="Object.keys(this.searchPhysicalStructure).length > 0" :value="searchPhysicalStructure"
                        :expand-depth=1000></JsonViewer>
                </td>
            </tr>
        </table>
            <button class="redButton" @Click="this.cancelInstallation" v-if="!compileGenerateBindingsLoading">Cancel</button>
        <button class="classicButton" @Click="this.goToExistingAppsBindingsOrCompile()"
            v-if="!compileGenerateBindingsLoading">Next</button>
    </div>
    <div class="installationStep" v-if="this.installationStep === stepBindingsExistingApps">
        <h2>Devices Bindings - existing apps (step 3/4)</h2>
        <p v-if="!compileGenerateBindingsLoading">Please check that bindings of currently installed apps are up to date.
        </p>
        <table class="bindingsTable" v-if="this.existingAppsBindings.length > 0 && !compileGenerateBindingsLoading">
            <tr>
                <td>Bindings</td>
                <td>Physical structure</td>
            </tr>
            <tr>
                <td></td>
                <td>
                    <input :value="this.searchField" placeholder="Search com objects"
                        @input="this.onSearchFieldChange($event)" />
                </td>
            </tr>
            <tr>
                <td valign="top">
                    <ul>
                        <li v-for='app in existingAppsBindings' :key="app.name">
                            <p class="appNameBindings">{{ app.name }}</p>
                            <ul>
                                <li v-for='b in app.bindings' :key="b.name">
                                    <div class="activeComObject comObject" @Click="this.selectedComObjectFilling = ''"
                                        v-if="this.selectedComObjectFilling === b">
                                        <p>Name: {{ b.name }} <br>
                                            Type: {{ b.binding.typeString }} <br>
                                            PhysicalID:
                                        </p>
                                        <v-select class="selectPhysId" :options="this.physicalPhysIds"
                                            :searchable="true" :filterable="true" v-model="b.binding.physDeviceId"
                                            :no-drop="false"></v-select>
                                    </div>
                                    <div class="inactiveComObject comObject" @Click="this.selectedComObjectFilling = b"
                                        v-else>
                                        <p>Name: {{ b.name }} <br>
                                            Type: {{ b.binding.typeString }} <br>
                                            PhysicalID:
                                        </p>
                                        <v-select class="selectPhysId" :options="this.physicalPhysIds"
                                            :searchable="true" :filterable="true" v-model="b.binding.physDeviceId"
                                            :no-drop="false"></v-select>
                                    </div>
                                </li>
                            </ul>
                        </li>
                    </ul>
                </td>
                <td>
                    <JsonViewer theme="my-awesome-json-theme" @keyclick="this.onKeyClick"
                        v-if="Object.keys(this.searchPhysicalStructure).length === 0" :value="filteredPhysicalStructure"
                        :expand-depth=3></JsonViewer>
                    <JsonViewer theme="my-awesome-json-theme" @keyclick="this.onKeyClick"
                        v-if="Object.keys(this.searchPhysicalStructure).length > 0" :value="searchPhysicalStructure"
                        :expand-depth=1000></JsonViewer>
                </td>
            </tr>
        </table>
        <button class="redButton" @Click="this.cancelInstallation" v-if="!compileGenerateBindingsLoading">Cancel</button>
        <button class="classicButton" @Click="this.goToNewAppsBindings()"
            v-if="!compileGenerateBindingsLoading">Back</button>


        <button class="classicButton" @click="compile" v-if="!compileGenerateBindingsLoading">Compile</button>
        <p v-if="compileGenerateBindingsLoading">Compiling...</p>
        <PulseLoader :color="this.$root.colourOrangeSvshi" v-if="compileGenerateBindingsLoading" />
    </div>
    <div class="installationStep" v-if="this.installationStep === stepLogs">
        <h2>Logs (step 4/4)</h2>
        <p class="sucessfulInstallationMsg"
            v-if="this.logs.includes('The apps have been successfully compiled and verified!')">SUCCESS: The new app(s)
            is(are) successfuly installed!</p>
        <p class="failedInstallationMsg" v-else>ERROR: The new app(s) was(were) not installed! Please see logs below:
        </p>
        <p>Compilation is done, please see logs below. Please be aware of the warnings:</p>
        <table>
            <tr>
                <td>
                    <textarea rows="30" cols="150" v-model="this.logs" readonly="true"></textarea>
                </td>
            </tr>
            <tr>
                <td>
                    <button class="classicButton" @Click="this.finishInstallationProcess()"
                        v-if="!compileGenerateBindingsLoading">Finish</button>
                </td>
            </tr>
        </table>



    </div>
    <!-- <h3>Update app</h3>
    <p>You can update an app that is already installed without redefining the bindings. To do so, upload this app
        <strong>ONLY</strong> in the generated folder.<br>
        Then select it in the list and click update.
    </p>
    <select v-model="this.appToUpdate" @change="this.refresh()">
        <option value="">Please select one</option>
        <option v-for="app in this.newApps" :key="app.id" :value="app.name">
            {{ app.name }}
        </option>
    </select>
    <button class="classicButton" @Click="this.updateApp" v-if="!updateLoading"
        :disabled="this.updateButtonDisabled">Update</button>
    <div>
        <PulseLoader :color="this.$root.colourOrangeSvshi" v-if="updateLoading" />
    </div> -->
    <!-- <Bindings ref="bindingsComp" /> -->
</template>

<style lang="scss">
@import '../assets/base.css';

.physicalStructureUploadInstructions {
    padding-bottom: 22px;
}

.uploadPhysDiv {
    padding: 22px;
}

p.appNameBindings {
    background-color: #e8c4a2;
    border-radius: 28px;
    padding-left: 12px;
    padding-right: 12px;
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    font-size: 22px;
}

table.bindingsTable {
    margin-left: auto;
    margin-right: auto;
}

.comObject {
    border-radius: 28px;
    padding-top: 20px;
    padding-bottom: 20px;
    padding-left: 40px;
    padding-right: 40px;
    margin-top: 12px;
    margin-bottom: 12px;
    cursor: pointer;
    width: 18em;
}

.activeComObject {
    background: linear-gradient(to bottom, #ffe2c6 5%, #e8c4a2 100%);

}

div.activeComObject:hover {
    background: linear-gradient(to bottom, #e8c4a2 5%, #ffe2c6 100%);
}

.inactiveComObject {
    background: linear-gradient(to bottom, #f7f7f7 5%, #dddddd 100%);
}

div.inactiveComObject:hover {
    background: linear-gradient(to bottom, #dddddd 5%, #f7f7f7 100%);
}

.sucessfulInstallationMsg {
    background-color: #2db051;
    color: white;
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    font-weight: bold;
    font-size: large;
    border-radius: 28px;
    text-align: center;
    margin: 0% 20% 20px;
    padding-top: 20px;
    padding-bottom: 20px;
}

.failedInstallationMsg {
    background-color: #dd2323;
    color: white;
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    font-weight: bold;
    font-size: large;
    border-radius: 28px;
    text-align: center;
    margin: 0% 20% 20px;
    padding-top: 20px;
    padding-bottom: 20px;
}

.installedAppsComp {
    margin-bottom: 12px;
    width: 60%;
    margin-left: auto;
    margin-right: auto;
}

.appToBeInstalled {
    background-color: #e8c4a2;
    border-radius: 28px;
    margin-left: auto;
    margin-right: auto;
    width: 60%;
}

input.addAppToInstalled {
    background-color: #dddddd;
    padding-top: 5px;
    padding-bottom: 5px;
}

h3.appToBeInstalled {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    margin-left: 22px;
    margin-right: 22px;
    padding-top: 12px;
}

ul.appToBeInstalled {
    padding-top: 16px;
    padding-bottom: 36px;
    padding-left: 32px;
    list-style-type: none;
}

li.appToBeInstalled {
    margin-left: 22px;
    width: 100%;
}

p.appToBeInstalled {
    font-size: large;
    background-color: #ffffff;
    padding-left: 48px;
    padding-right: 48px;
    border-radius: 28px;
    border-color: #000;
    border-style: solid;
    border-width: 1px;
    margin-right: 22px;
}

p.warning {
    font-weight: bold;
}

.my-awesome-json-theme {
    background: #fff;
    white-space: nowrap;
    color: #525252;
    font-size: 16px;
    font-family: Verdana, Geneva, Tahoma, sans-serif;

    .jv-ellipsis {
        color: #999;
        background-color: #eee;
        display: inline-block;
        line-height: 0.9;
        font-size: 0.9em;
        padding: 0px 6px 2px 6px;
        border-radius: 3px;
        vertical-align: 2px;
        cursor: pointer;
        user-select: none;
    }

    .jv-button {
        color: #49b3ff
    }

    //   [keyName="address"] { color: #c0e944 }
    .jv-key {
        color: #262626;
        cursor: pointer;
    }

    .jv-item {
        &.jv-array {
            color: #111111
        }

        &.jv-boolean {
            color: #5f5f5f
        }

        &.jv-function {
            color: #5f5f5f
        }

        &.jv-number {
            color: #5f5f5f
        }

        &.jv-number-float {
            color: #fc1e70
        }

        &.jv-number-integer {
            color: #8dba3f
        }

        &.jv-object {
            color: #111111
        }

        &.jv-undefined {
            color: #5f5f5f
        }

        &.jv-string {
            color: #5f5f5f;
            word-break: break-word;
            white-space: normal;
        }
    }

    .jv-code {
        .jv-toggle {
            &:before {
                padding: 0px 2px;
                border-radius: 2px;
            }

            &:hover {
                &:before {
                    background: #eee;
                }
            }
        }
    }
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
