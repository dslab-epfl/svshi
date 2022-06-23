<script lang="jsx" >
import * as http from "http";
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'


export default {
    components: {
        PulseLoader,
    },
    data() {
        return {
            newApps: [],
            isRunning: false,
        }
    },
    methods: {
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
        updateNewAppToInstall(evt) {
            if (this.$refs.newAppToInstall.files.length > 0) {
                let file = this.$refs.newAppToInstall.files[0]
                console.log(file)
                if (!file.name.endsWith(".zip")) {
                    alert("Please upload a .zip file!")
                    this.$refs.knxProjFile.value = ""
                } else {
                    this.zipFileUploadNewApps = file
                }
            } else {
                this.zipFileUploadNewApps = ''
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
        async deleteAllInGenerated(confirmation = true) {
            if (!confirmation || confirm("This cannot be undone! Are you sure you want to delete all apps to be installed?")) {
                let response = await fetch(this.$root.backendServerAddress + "/deleteAllGenerated/", {
                    method: 'POST',
                    headers: {
                        'Accept': 'application/json'
                    }
                })
                let responseBody = await response.json();
                if (!responseBody.status) {
                    var message = "An error occurred while deleting all files/directories! Please see the following logs: "
                    let array = responseBody.output
                    array.forEach(element => {
                        message += "\n"
                        message += element
                    });
                    alert(message)
                }
            }
        },
        async uploadToGenerated() {
            if (this.zipFileUploadNewApps === '') {
                alert("Please select a zip file before!")
            } else {
                let formData = new FormData()
                formData.append('method', 'POST')
                formData.append('icon', this.zipFileUploadNewApps)
                let response = await fetch(this.$root.backendServerAddress + "/generated", {
                    method: 'POST',
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'multipart/form-data'
                    },
                    body: formData
                })
                let responseBody = await response.json();
                if (!responseBody.status) {
                    var message = "An error occurred while uploading '" + this.zipFileToUpload.name + "'! Please see the following logs: "
                    let array = responseBody.output
                    array.forEach(element => {
                        message += "\n"
                        message += element
                    });
                    alert(message)
                }

                this.zipFileUploadNewApps = ''

            }
        },
        async downloadFromGenerated(filename) {
            let zipname = filename + ".zip"
            let url = this.$root.backendServerAddress + "/generated/" + filename

            this.downloadFile(url, zipname)

        },
        async downloadAllNewAppsGenerated() {
            let filename = "appsToBeInstalled.zip"
            await this.deleteInGenerated("apps_bindings.json", false)
            await this.deleteInGenerated("physical_structure.json", false)
            let url = this.$root.backendServerAddress + "/generated"
            this.downloadFile(url, filename)
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
        async refresh() {
            this.isRunning = await this.isSvshiRunning()
            this.newApps = await this.getNewApps()

        }
    },
    mounted() {
        this.refresh()
    }
}
</script>

<template>
    <div class="appToBeInstalled">
        <table>
            <tr>
                <td>
                    <h3 class="appToBeInstalledTitle">Apps to be installed</h3>
                </td>
                <td>
                    <button class="redButton deleteAll" v-if="!this.isRunning && this.newApps.length > 0"
                        @Click="deleteAllInGenerated">
                        <FontAwesomeIcon icon="trash-can" /> ALL
                    </button>
                </td>
                <td>
                    <button class="classicButton downloadAll" v-if="this.newApps.length > 0"
                        @Click="downloadAllNewAppsGenerated">
                        <FontAwesomeIcon icon="download" /> All files
                    </button>
                </td>
                <td>
                    <input type="file" id="file" ref="newAppToInstall" class="addAppToInstalled"
                        @change="updateNewAppToInstall">
                </td>
                <td>
                    <button class="greenButton uploadTo" v-if="true" @Click="uploadToGenerated()">
                        <FontAwesomeIcon icon="upload" />
                    </button>
                </td>
            </tr>
        </table>

        <ul class="appsList">
            <li v-if="this.newApps.length > 0" v-for='app in newApps' :key="app.id">
                <table>
                    <tr>
                        <td>
                            <p class="appName">{{ app.name }}</p>
                        </td>
                        <td>
                            <button class="redButton" v-if="!this.isRunning" @Click="deleteInGenerated(app.name)">
                                <FontAwesomeIcon icon="trash-can" />
                            </button>
                        </td>
                        <td>
                            <button class="classicButton" v-if="!app.deleting" @Click="downloadFromGenerated(app.name)">
                                <FontAwesomeIcon icon="download" /> files
                            </button>

                        </td>
                    </tr>
                </table>
            </li>
            <li v-else>
                <p class="appName">No apps to be installed, upload or generate some first.</p>
            </li>
        </ul>
    </div>
</template>

<style>
@import '../assets/base.css';

.appToBeInstalled {
    background-color: #e8c4a2;
    border-radius: 28px;
}


ul.appsList {
    padding-top: 16px;
    padding-bottom: 36px;
    padding-left: 32px;
    list-style-type: none;
}

.downloadAll {
    margin-top: 12px;
}

.deleteAll {
    margin-top: 12px;
    margin-left: 12px;
}

input.addAppToInstalled {
    margin-top: 12px;
}

button.uploadTo {
    margin-top: 12px;
}

h3.appToBeInstalledTitle {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    margin-left: 22px;
    margin-right: 22px;
    padding-top: 12px;
}

p.appName {
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
</style>
