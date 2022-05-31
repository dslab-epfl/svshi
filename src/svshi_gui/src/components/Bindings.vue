<script lang="js">
import JsonViewer from 'vue-json-viewer'
import vSelect from "vue-select";
import JSZip from 'jszip'
import 'vue-select/dist/vue-select.css';
import 'vue-json-viewer/style.css'

let successStatusCode = 200
export default {
    components: { JsonViewer, "v-select": vSelect },
    data() {
        return {
            "appBindings": [],
            "physicalStructure": {},
            "filteredPhysicalStructure": {},
            "searchPhysicalStructure": {},
            "searchField": "",
            "physicalPhysIds": [],
            "showBindings": true,
            "test": -1
        }
    },
    methods: {
        bindingsReady() {
            let usedIds = this.appBindings.flatMap(app => app.bindings.map(b => b.binding.physDeviceId))
            return !usedIds.includes(-1) && !usedIds.some(i => !this.physicalPhysIds.includes(i))
        },
        async refresh() {
            await this.getBindings()
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
                    console.log("Bindings not generated!")
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
                    console.log(d.name, d.name.includes(this.searchField))
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
        onSearchFieldChange(evt) {
            this.searchField = evt.target.value
            this.updateSearchPhysicalStructure()
        }
    },
    mounted() {
        this.refresh()
    }
}
</script>
<template>
    <h2>Bindings</h2>
    <div class="section">
        <button @Click="this.refresh">Refresh bindings</button>
        <table v-if="this.appBindings.length > 0">
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
                        <li v-for='app in this.appBindings' :key="app.name">
                            {{ app.name }}
                            <ul>
                                <li v-for='b in app.bindings' :key="b.name">
                                    <p>Name: {{ b.name }} <br>
                                        Type: {{ b.binding.typeString }} <br>
                                        PhysicalID:
                                    </p>
                                    <v-select :options="this.physicalPhysIds" :searchable="true" :filterable="true"
                                        v-model="b.binding.physDeviceId" :no-drop="false"></v-select>
                                    <!-- <select v-model="b.binding.physDeviceId">
                                    <option disabled value=-1>Please select one</option>
                                    <option v-for="i in this.physicalPhysIds" :value=i>
                                        {{ i }}
                                    </option>
                                </select> -->
                                </li>
                            </ul>
                        </li>
                    </ul>
                </td>
                <td>
                    <json-viewer v-if="Object.keys(this.searchPhysicalStructure).length === 0"
                        :value="filteredPhysicalStructure" :expand-depth=3></json-viewer>
                    <json-viewer v-if="Object.keys(this.searchPhysicalStructure).length > 0"
                        :value="searchPhysicalStructure" :expand-depth=1000></json-viewer>
                </td>
            </tr>
        </table>
    </div>
</template>

<style>
@import '../assets/base.css';
</style>
