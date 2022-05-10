<script lang="js">
export default {

    data() {
        return {
            zipFileToUpload: '',
            generatedFolderItems: [],
            appToDownload: "",
            installedApps: []
        }
    },
    methods: {
        updateUploadFile: function (evt) {
            if (this.$refs.zipFileToUpload.files.length > 0) {
                let file = this.$refs.zipFileToUpload.files[0]
                if (file.type !== "application/zip") {
                    alert("Please upload a zip file!")
                    this.$refs.zipFileToUpload.value = ""
                } else {
                    this.zipFileToUpload = file
                }
            } else {
                this.zipFileToUpload = ''
            }
        },
        async downloadCurrentGenerated() {
            let filename = "currentGenerated.zip"
            let url = this.$root.backendServerAddress + "/generated"

            this.downloadFile(url, filename)

        },
        isAppNameInstalled: function (appName) {
            let filtered = this.installedApps.filter(a => a.name === appName)
            return filtered.length !== 0
        },
        async downloadInstalledApp(appName) {
            if (this.isAppNameInstalled(appName)) {
                let filename = "installedApp_" + appName + ".zip"
                let url = this.$root.backendServerAddress + "/installedApp/" + appName

                this.downloadFile(url, filename)
            } else {
                alert("Please select a valid installed app name!")
            }
        },
        async downloadAllInstalledApps() {
            let filename = "installedApps.zip"
            let url = this.$root.backendServerAddress + "/allInstalledApps"

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
        async uploadGenerated() {
            if (this.zipFileToUpload === '') {
                alert("Please select a zip file before!")
            } else {
                let formData = new FormData()
                formData.append('method', 'POST')
                formData.append('icon', this.zipFileToUpload)
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
                    alert("New files were uploaded successfuly!")
                } else {
                    var message = "An error occurred while uploading '" + this.zipFileToUpload.name + "'! Please see the following logs: "
                    let array = responseBody.output
                    array.forEach(element => {
                        message += "\n"
                        message += element
                    });
                    alert(message)
                }
            }
        },
        async deleteGenerated() {
            if (confirm("This will delete EVERYTHING in the generated folder! This CANNOT be undone!\nAre you sure?")) {
                let response = await fetch(this.$root.backendServerAddress + "/deleteGenerated", {
                    method: 'POST',
                    headers: {
                        'Accept': 'application/json'
                    }
                })
                let responseBody = await response.json();
                if (responseBody.status) {
                    alert("Generated folder was successfully emptied!")
                } else {
                    var message = "An error occurred while deleting generated folder! Please see the following logs: "
                    let array = responseBody.output
                    array.forEach(element => {
                        message += "\n"
                        message += element
                    });
                    alert(message)
                }
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
        async loadInstalledApps() {
            this.installedApps = await this.getInstalledApps()
        },
        async refresh() {
            this.loadInstalledApps()
        }
    },
    mounted() {
    }
}
</script>
<template>
    <h2>File management</h2>
    <p>Here you can manage files present on SVSHI</p>

    <h3>Generated folder:</h3>
    <button @Click="downloadCurrentGenerated">Download current generated</button>

    <p>Select a .zip file containing what you want to upload to the generated folder on SVSHI</p>
    <input type="file" id="zipFileToUpload" ref="zipFileToUpload" class="custom-file-input" @change="updateUploadFile">
    <button @Click="uploadGenerated">Upload to generated</button>

    <p>Delete everything in the generate folder. WARNING: this cannot be undone!</p>
    <button @Click="deleteGenerated">Delete generated folder</button>

    <h3>Installed apps</h3>
    <p>Here you can download the apps' code that are currently installed on the system to update them or check them.</p>
    <div>
        <select v-model="appToDownload">
            <option disabled value="">Please select one</option>
            <option v-for="app in this.$root.installedApps" :value="app.name">
                {{ app.name }}
            </option>
        </select>
        <button @Click="downloadInstalledApp(appToDownload)">Download selected app</button>
    </div>
    <div>
        <p>Download all the installed apps along with the bindings and the physical structure. This can be uploaded to generated and compiled as is.</p>
        <button @Click="downloadAllInstalledApps">Download all installed apps</button>
    </div>

    <!-- 
        https://codesandbox.io/s/boring-leaf-v7b2s?file=/src/App.vue
    -->
    <!-- <div id="generatedFolder">
        <vue3-tree-vue :items="items" :isCheckable="false" :hideGuideLines="false" v-model:selectedItem="selectedItem">
            Applying some simple styling to tree-items
            <template v-slot:item-prepend-icon="treeViewItem">
                <img src="./assets/folder.svg" alt="folder" v-if="treeViewItem.type === 'folder'" height="20"
                    width="20" />
            </template>
        </vue3-tree-vue>
    </div> -->
</template>

<style>
@import '../assets/base.css';
</style>
