<script lang="js">
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'
import Bindings from './Bindings.vue'
import JSZip from 'jszip'

export default {
    components: {
        PulseLoader,
        Bindings
    },
    data() {
        return {
            knxProjFile: '',
            compileGenerateBindingsLoading: false,
            updateLoading: false,
            newApps: [],
            appToUpdate: "",
            installedApps: [],
            updateButtonDisabled: false
        }
    },
    methods: {
        updateFile(evt) {
            if (this.$refs.knxProjFile.files.length > 0) {
                let file = this.$refs.knxProjFile.files[0]
                console.log(file)
                if (!file.name.endsWith(".knxproj")) {
                    alert("Please upload a .knxproj file!")
                    this.$refs.knxProjFile.value = ""
                } else {
                    this.knxProjFile = file
                }
            } else {
                this.knxProjFile = ''
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
            if (this.$refs.bindingsComp.bindingsReady()) {
                await this.$refs.bindingsComp.uploadNewBindings()
                let success = await this.executeAction("compile")
                if (success) {
                    this.$refs.bindingsComp.appBindings = []
                }
            } else {
                alert("Please fill the bindings below before compiling!")
            }
        },
        async generateBindings() {
            if (await this.isSvshiRunning()) {
                alert("You cannot generateBindings while SVSHI is running!")
                return
            }
            this.executeAction("generateBindings")
        },
        async executeAction(action) {
            if (action === "compile" || action === "generateBindings") {
                if (this.knxProjFile === '') {
                    alert("Please upload a .knxproj file before!")
                } else {
                    try {
                        let zip = JSZip()
                        zip.file(this.knxProjFile.name, this.knxProjFile)
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

                        let responseBody = await response.json();
                        var message = ""
                        if (responseBody.status) {
                            if (action === "compile") {
                                message += "Compilation was successful!"
                            } else {
                                message += "Bindings were successfully generated!"
                            }
                            this.$refs.bindingsComp.refresh()
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
                        if (!responseBody.status) {
                            alert(message)
                            return false
                        } else {
                            console.log(message)
                            return true
                        }
                    } catch (error) {
                        console.log(error)
                        this.compileGenerateBindingsLoading = false
                    }
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
                        this.$refs.bindingsComp.refresh()
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
        async refresh() {
            this.updateButtonDisabled = !this.updatable()
            this.newApps = await this.getNewApps()
            this.installedApps = await this.getInstalledApps()
            // this.$refs.bindingsComp.refresh()
        }
    },
    mounted() {
        this.refresh()
    }
}
</script>

<template>
    <h2>Generation and Compilation</h2>
    <p>Please upload a KNX ETS project file (extension .knxproj):</p>
    <input type="file" id="file" ref="knxProjFile" class="custom-file-input" @change="updateFile">
    <h3>App to be installed</h3>
    <ul>
        <li v-for='app in this.newApps' :key="app.id" v-if="this.newApps.length > 0">
            {{ app.name }}
        </li>
        <li v-else>No apps to be installed, upload some to the generated folder</li>
    </ul>


    <button @Click="generateBindings" v-if="!compileGenerateBindingsLoading">Generate bindings</button>
    <button @click="compile" v-if="!compileGenerateBindingsLoading">Compile</button>
    <div>
        <PulseLoader :color="this.$root.colourOrangeSvshi" v-if="compileGenerateBindingsLoading" />
    </div>
    <h3>Update app</h3>
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
    <button @Click="this.updateApp" v-if="!updateLoading" :disabled="this.updateButtonDisabled">Update</button>
    <div>
        <PulseLoader :color="this.$root.colourOrangeSvshi" v-if="updateLoading" />
    </div>
    <Bindings ref="bindingsComp" />
</template>

<style>
@import '../assets/base.css';
</style>
