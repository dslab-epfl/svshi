<script lang="jsx" >
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'


export default {
    components: {
        PulseLoader,
    },
    data() {
        return {
            apps: [],
            showSuccessMark: false,
            allAppsSelection: false,
        }
    },
    computed: {
        selectedAppNames() {
            let res = []
            for (let i = 0; i < this.apps.length; i++) {
                const app = this.apps[i];
                if (app.selected) {
                    res.push(app.name)
                }
            }
            return res
        },
        allAppsSelectedInArray() {

            for (let i = 0; i < this.apps.length; i++) {
                const app = this.apps[i];
                if (!app.selected) {
                    return false
                }
            }
            return true
        },
        allAppsUnselectedInArray() {
            for (let i = 0; i < this.apps.length; i++) {
                const app = this.apps[i];
                if (app.selected) {
                    return false
                }
            }
            return true
        }
    },
    methods: {
        sleep(ms) {
            return new Promise(
                resolve => setTimeout(resolve, ms)
            )
        },
        async getApps() {
            try {
                let res = await fetch(this.$root.backendServerAddressPort + "/applications/names");
                let array = await res.json();
                let currentId = 0
                let ret = []
                array.forEach(element => {
                    ret.push({ id: currentId++, name: element, selected: false })
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
                    this.$refs.newAppToInstall.value = null
                } else {
                    this.zipFileUploadApps = file
                }
            } else {
                this.zipFileUploadApps = ''
            }
        },
        async deleteApp(filename, confirmation = true) {
            if (!confirmation || confirm("This cannot be undone! Are you sure you want to delete '" + filename + "' from your session? This cannot be undone, files will be deleted!")) {
                let response = await fetch(this.$root.backendServerAddressPort + "/applications/delete/" + filename, {
                    method: 'POST',
                    headers: {
                        'Accept': 'application/json'
                    }
                })
                if (response.status !== 200) {
                    var message = "An error occurred while deleting the requested app! Please see the following logs: "
                    message += "\n"
                    message += await response.text()
                    alert(message)
                }
            }
            this.refresh()
        },
        async deleteAllApps(confirmation = true) {
            if (!confirmation || confirm("This cannot be undone! Are you sure you want to delete ALL apps from your session? This cannot be undone, files will be deleted!")) {
                for (let i = 0; i < this.apps.length; i++) {
                    const app = this.apps[i];
                    this.deleteApp(app.name, confirmation = false)

                }
            }
        },
        async uploadNewApp() {
            if (this.zipFileUploadApps === '' || typeof this.zipFileUploadApps === 'undefined') {
                alert("Please select a zip file before!")
            } else {
                let appName = this.zipFileUploadApps.name.replace(".zip", "")
                console.log("uploading '" + appName + "'")
                let formData = new FormData()
                formData.append('method', 'POST')
                formData.append('icon', this.zipFileUploadApps)
                let response = await fetch(this.$root.backendServerAddressPort + "/applications/add/" + appName, {
                    method: 'POST',
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'multipart/form-data'
                    },
                    body: formData
                })
                if (response.status !== 200) {
                    var message = "An error occurred while uploading '" + appName + "'! Please see the following logs: "
                    message += "\n"
                    message += await response.text()
                    alert(message)
                } else {
                    this.showSucess()
                }

                this.zipFileUploadApps = ''
                this.$refs.newAppToInstall.value = null

            }
            this.refresh()
        },
        async showSucess() {
            this.showSuccessMark = true
            this.$refs.uploadButton.disabled = true
            await this.sleep(2000)
            this.showSuccessMark = false
            this.$refs.uploadButton.disabled = false

        },
        async downloadApp(appName) {
            let zipname = appName + ".zip"
            let url = this.$root.backendServerAddressPort + "/applications/files/" + appName

            this.downloadFile(url, zipname)

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
        onAllAppsCheckBoxChange() {
            if (this.allAppsSelection) {
                for (let i = 0; i < this.apps.length; i++) {
                    this.apps[i].selected = true
                }
            } else {
                for (let i = 0; i < this.apps.length; i++) {
                    this.apps[i].selected = false
                }
            }
            this.passSelectedAppsToParent()
        },
        onIndividualCheckBoxChange() {
            if (this.allAppsSelectedInArray) {
                this.allAppsSelection = true
            } else {
                this.allAppsSelection = false
            }
            this.passSelectedAppsToParent()
        },
        passSelectedAppsToParent() {
            this.$emit("onSelectApp", this.selectedAppNames)
        },
        async refresh() {
            this.apps = await this.getApps()

        }
    },
    mounted() {
        this.refresh()
    }
}
</script>

<template>
    <div class="appList">
        <table>
            <tr>
                <td>
                    <div id="checkBoxAllApps">
                        <div class="control-group">
                            <label class="control control-checkbox">
                                <input id="appGenPriviledgedCheckbox" type="checkbox" v-model="allAppsSelection"
                                    @change="this.onAllAppsCheckBoxChange()" />
                                <div class="control_indicator"></div>
                            </label>
                        </div>
                    </div>
                </td>

                <td>
                    <input type="file" id="file" ref="newAppToInstall" class="addAppToInstalled"
                        v-if="!this.showSuccessMark" @change="updateNewAppToInstall">
                </td>
                <td>
                    <p v-if="!this.showSuccessMark" id="addNewAppText">Upload new app</p>
                </td>
                <td>
                    <button ref="uploadButton" class="greenButton uploadTo" @Click="uploadNewApp()">
                        <FontAwesomeIcon v-if="this.showSuccessMark" icon="check" />
                        <FontAwesomeIcon v-else icon="upload" />
                    </button>
                </td>
                <td>
                    <button class="redButton deleteAll" v-if="this.apps.length > 0" @Click="deleteAllApps">
                        <FontAwesomeIcon icon="trash-can" /> ALL
                    </button>
                </td>
            </tr>
        </table>

        <ul class="appsList">
            <li v-if="this.apps.length > 0" v-for='app in apps' :key="app.id">
                <table>
                    <tr>
                        <td>
                            <div class="selectBox">
                                <div class="control-group">
                                    <label class="control control-checkbox">
                                        <input id="appGenPriviledgedCheckbox" type="checkbox" v-model="app.selected"
                                            @change="this.onIndividualCheckBoxChange()" />
                                        <div class="control_indicator"></div>
                                    </label>
                                </div>
                            </div>
                        </td>
                        <td>
                            <p class="appName">{{ app.name }}</p>
                        </td>
                        <td>
                            <button class="redButton" @Click="deleteApp(app.name)">
                                <FontAwesomeIcon icon="trash-can" />
                            </button>
                        </td>
                        <td>
                            <button class="classicButton" @Click="downloadApp(app.name)">
                                <FontAwesomeIcon icon="download" /> files
                            </button>

                        </td>
                    </tr>
                </table>
            </li>
            <li v-else>
                <p class="appName">No apps, upload apps that you have written locally in a zip archive.</p>
            </li>
        </ul>
    </div>
</template>

<style>
@import '../assets/base.css';

#addNewAppText {
    padding-top: 12px;
    font-weight: bold;
}

.selectBox {
    padding-bottom: 18px;
}

.appList {
    background-color: #f4d4b5;
    border-radius: 28px;
    border-color: rgb(0, 0, 0);
    border-style: solid;
    border-width: 1px;
    margin: 12px;
    padding: 12px;
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

h3.appListTitle {
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
