<script lang="jsx" >
import * as http from "http";
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'


export default {
    components: {
        PulseLoader,
    },
    data() {
        return {
            installedApps: [],
            isRunning: false,
            uninstallInProgress: false,
            allAppsUninstalling: false,
            allAppsName: "42All"
        }
    },
    methods: {
        async getAppsList() {
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
        async loadAppsList() {
            this.installedApps = await this.getAppsList()
        },
        async deleteApp(appName) {
            if (await this.isSvshiRunning()) {
                alert("You cannot remove apps while SVSHI is running!")
                return
            }
            console.log(appName)
            if (appName === "42All") {
                if (confirm("Are you sure you want to uninstall ALL applications?")) {
                    try {
                        this.allAppsUninstalling = true
                        const requestOptions = {
                            method: "POST"
                        };
                        this.uninstallInProgress = true
                        let res = await fetch(this.$root.backendServerAddress + "/removeAllApps", requestOptions);
                        let responseBody = await res.json();
                        if (responseBody.status) {
                            console.log("Removing all the apps was successful!")
                        } else {
                            console.log("An error occurred while removing all the apps! Please see the following logs: ")
                            let array = responseBody.output
                            array.forEach(element => {
                                console.log(element)
                            });
                        }
                        this.allAppsUninstalling = false
                    } catch (error) {
                        console.log(error);
                    }
                }
            }
            else if (this.installedApps.some(a => a.name === appName)) {
                if (confirm("Are you sure you want to uninstall '" + appName + "'?")) {
                    try {
                        this.installedApps = this.installedApps.map(a =>
                            a.name === appName
                                ? { ...a, deleting: true }
                                : a
                        );
                        const requestOptions = {
                            method: "POST"
                        };
                        this.uninstallInProgress = true
                        let res = await fetch(this.$root.backendServerAddress + "/removeApp/" + appName, requestOptions);
                        let responseBody = await res.json();
                        if (responseBody.status) {
                            console.log("Removing the app '" + appName + "' was successful!")
                        } else {
                            console.log("An error occurred while removing '" + appName + "'! Please see the following logs: ")
                            let array = responseBody.output
                            array.forEach(element => {
                                console.log(element)
                            });
                        }
                    } catch (error) {
                        console.log(error);
                    }
                }
            }
            this.uninstallInProgress = false
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
            if (!this.uninstallInProgress) {
                this.isRunning = await this.isSvshiRunning()
                this.loadAppsList()
            }
        }
    },
    mounted() {
        this.refresh()
    }
}
</script>

<template>
    <div class="installedAppsComponent">
        <table>
            <tr>
                <td>
                    <h3 class="installedAppTitle">Installed apps</h3>
                </td>
                <td>
                    <button class="redButton uninstallAll"
                        v-if="!this.uninstallInProgress && !this.isRunning && this.installedApps.length > 0"
                        @Click="deleteApp('42All')">
                        <FontAwesomeIcon icon="trash-can" /> ALL
                    </button>
                </td>
                <td>
                    <button class="classicButton downloadAll"
                        v-if="this.installedApps.length > 0 && !this.uninstallInProgress && !this.isRunning"
                        @Click="downloadAllInstalledApps()">
                        <FontAwesomeIcon icon="download" /> All files
                    </button>

                </td>
            </tr>
        </table>
        <div v-if="this.allAppsUninstalling">
            <PulseLoader :color="this.colourOrangeSvshi" />
        </div>

        <ul class="appsList">
            <li v-if="this.installedApps.length > 0" v-for='app in installedApps' :key="app.id">
                <table>
                    <tr>
                        <td>
                            <p class="appName">{{ app.name }}</p>
                        </td>
                        <td>
                            <button class="redButton" v-if="!this.uninstallInProgress && !this.isRunning"
                                @Click="deleteApp(app.name)">
                                <FontAwesomeIcon icon="trash-can" />
                            </button>
                            <div v-if="app.deleting">
                                <PulseLoader :color="this.$root.colourOrangeSvshi" v-if="app.deleting" />
                            </div>
                        </td>
                        <td>
                            <button class="classicButton" v-if="!app.deleting" @Click="downloadInstalledApp(app.name)">
                                <FontAwesomeIcon icon="download" /> files
                            </button>

                        </td>
                    </tr>
                </table>
            </li>
            <li v-else>
                <p class="appName">No apps currently installed.</p>
            </li>
        </ul>
    </div>
</template>

<style>
@import '../assets/base.css';

.installedAppsComponent {
    background-color: #e8c4a2;
    border-radius: 28px;
}

ul.appsList {
    padding-top: 16px;
    padding-bottom: 36px;
    padding-left: 32px;
    list-style-type: none;
}

.uninstallAll {
    margin-top: 12px;
    margin-left: 52px;
}

.downloadAll {
    margin-top: 12px;
}

h3.installedAppTitle {
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
