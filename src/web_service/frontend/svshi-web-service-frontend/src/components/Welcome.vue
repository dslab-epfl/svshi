<script lang="jsx">
import { defineComponent } from 'vue'
import JSZip from 'jszip'

export default defineComponent({
    data() {
        return {
            floors: [],
            etsFileZipBlob: "",
        }
    },
    computed: {
        floorIds: function () {
            return this.floors.map(f => f.id)
        }
    },
    methods: {
        async updateEtsFile(event) {
            let files = event.target.files
            if (files.length === 1) {
                let f = files[0]
                console.log(f)
                if (f.name.endsWith(".knxproj")) {
                    this.etsFileZipBlob = await this.createZipFileBlob(f.name, f)
                } else {
                    alert("Please select a .knxproj file!")
                    this.etsFileZipBlob = ""
                }
            }
        },
        async updateFloorFile(floorId, event) {
            if (this.floorIds.includes(floorId)) {

                let files = event.target.files
                if (files.length === 1) {
                    let f = files[0]
                    console.log(f)
                    if (f.name.endsWith(".dxf")) {
                        let toUpdate = this.floors.find(f => f.id === floorId)
                        if (toUpdate !== undefined) {
                            toUpdate.cadFileZip = await this.createZipFileBlob(toUpdate.name, f)
                        }
                    } else {
                        alert("Please select a .dxf file!")
                    }
                }
            }
            console.log(this.floors)
        },
        addFloor() {
            let newId = this.floorIds.length > 0 ? Math.max(...this.floorIds) + 1 : 0
            let newFloor = { id: newId, name: "floor " + newId }
            this.floors.push(newFloor)
        },
        removeFloor(floorId) {
            this.floors = this.floors.filter(f => f.id !== floorId)
        },
        async continue() {
            // if (this.floors.length < 1) {
            //     alert("Please add at least one floor!")
            //     return
            // }
            if (this.etsFileZipBlob === "") {
                alert("Please upload a knxproj file!")
                return
            }
            var flag = true
            // flag = flag && await this.exportFloors()
            flag = flag && await this.exportEtsFile()
            if (!flag) {
                alert("An error occured while uploading data, please try again later...")
            } else {
                this.$emit("continue")
            }
        },
        async reset() {
            if (confirm("Are you sure you want to reset your session? Everything (apps, ETS file, ...) will be lost!")) {
                let url = this.$root.backendServerAddressPort + "/reset/session"
                let response = await fetch(url, {
                    method: 'POST',
                    headers: {
                        'Accept': 'application/json',
                    },
                })
                await this.$root.getOrRefreshSession()
                await this.load()
                this.refresh()
            }
        },
        async exportFloors() {
            var flag = true
            for (var floor of this.floors) {
                flag = flag && await this.exportFloor(floor)
            }
            return flag

        },
        async exportFloor(f) {
            let formData = new FormData()
            let url = this.$root.backendServerAddressPort + "/addFloor/" + f.name + "/" + f.id
            formData.append('method', 'POST')
            formData.append("icon", f.cadFileZip)
            let response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'multipart/form-data',
                },
                body: formData
            })
            if (response.status != 200) {
                console.log(`Error occured when exporting floor ${f}. \nError is: ${await response.text()}`)
                return false
            }
            return true
        },
        async exportEtsFile() {
            let formData = new FormData()
            let url = this.$root.backendServerAddressPort + "/etsFile"
            formData.append('method', 'POST')
            formData.append("icon", this.etsFileZipBlob)
            let response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'multipart/form-data',
                },
                body: formData
            })
            if (response.status != 200) {
                console.log(`Error occured when exporting ets file.\nError is: ${await response.text()}`)
                return false
            }
            return true
        },
        async createZipFileBlob(zipFilename, file) {
            let zip = JSZip()
            zip.file(zipFilename, file)
            let zipFile = new Blob([await zip.generateAsync({ type: "uint8array" })])
            return zipFile
        },
        async getFloorsFromBackendAndReplace() {
            let url = this.$root.backendServerAddressPort + "/floors"
            let response = await fetch(url, {
                method: 'GET',
            })
            if (response.status != 200) {
                console.log(`Error occured when getting floors.\nError is: ${response.text()}`)
            } else {
                this.floors = []
                let floorsJson = await response.json()
                console.log(floorsJson)
                console.log(floorsJson.floors)
                for (var i in floorsJson.floors) {
                    console.log(floorsJson.floors[i])
                    let fj = floorsJson.floors[i]
                    let fBlob = await this.getFloorFile(fj.number)
                    let newFloor = { id: fj.number, name: fj.name, cadFileZip: fBlob }
                    this.floors.push(newFloor)
                }
            }
        },
        setFileInInputElement() {
            // var floorInputs = document.getElementsByClassName("floorFileInput")
            // var len = floorInputs.length
            // console.log(len)
            // for (var n = 0; n < len; n++) {
            //     console.log(floorInputs[n].files)
            //     const dt = new DataTransfer();
            //     var file = new File([this.floors[n].cadFileZip], "your_current_file.dxf");
            //     dt.items.add(file)
            //     floorInputs[n].files = dt.files
            // }

            let etsInput = document.getElementById("etsFileInput")
            if (this.etsFileZipBlob !== null) {
                const dt = new DataTransfer();
                var file = new File([this.etsFileZipBlob], "your_current_ets_file.knxproj");
                dt.items.add(file)
                etsInput.files = dt.files
            } else {
                etsInput.value = ""
            }

        },
        async getFloorFile(floorId) {
            let url = this.$root.backendServerAddressPort + "/floorFile/" + floorId
            let response = await fetch(url, {
                method: 'GET',
            })
            if (response.status != 200) {
                console.log("No floor with id = '" + floorId + "' in the current session!")
                return null
            } else {
                let floorFileBlob = await this.createZipFileBlob("your_current_floor_file.zip", new File([await response.blob()], "your_current_floor_file.knxproj"))
                return floorFileBlob
            }
        },
        async getEtsFile() {
            let url = this.$root.backendServerAddressPort + "/etsFile/"
            let response = await fetch(url, {
                method: 'GET',
            })
            if (response.status != 200) {
                console.log("Error while getting the Ets file! Error was: " + await response.text())
                return null
            } else {
                let etsFileBlob = await this.createZipFileBlob("your_current_ets_file.zip", new File([await response.blob()], "your_current_ets_file.knxproj"))
                return etsFileBlob
            }
        },
        refresh() {

        },
        async load() {
            // await this.getFloorsFromBackendAndReplace()
            this.etsFileZipBlob = await this.getEtsFile()
            this.setFileInInputElement()

        }
    },
    mounted() {
        this.load()
        this.refresh()
    }
})
</script>

<template>
    <div class="titlePage">
        <img class="logoSvshiTitlePage" src='../assets/svshi_logo.png' />
        <h1 class="titlePage">Welcome on the SVSHI companion service!</h1>
        <p class="titlePage">This service will help you to discover what SVSHI could bring to your own
            installation.<br />You can upload your devices.<br />You will be able to develop
            apps
            and test them live with our simulator.</p>
        <!-- <p class="titlePage">To start, please upload a CAD file (.dxf) for each floor of your building:</p> -->
        <!-- <div class="floorsListDiv">
            <ul>
                <li class="floorsCadFilesList" v-for='floor in this.floors' :key="floor.id">
                    <table>
                        <tr>
                            <td>
                                <p>{{ floor.id }}</p>
                            </td>
                            <td>
                                <input v-model="floor.name" placeholder="floor name" />
                            </td>
                            <td>
                                <input type="file" id="floorFile" class="custom-file-input floorFileInput"
                                    @change="this.updateFloorFile(floor.id, $event)">
                            </td>
                            <td>
                                <button class="redButton" @Click="this.removeFloor(floor.id)">
                                    <FontAwesomeIcon icon="trash-can" />
                                </button>
                            </td>
                        </tr>
                    </table>
                </li>
            </ul>
            <button class="greenButton addFloor" @Click="this.addFloor">
                <FontAwesomeIcon icon="plus" />
            </button>
        </div> -->

        <p class="titlePage">To start, please upload one ETS Project file (.knxproj) to import your devices:</p>
        <div class="etsFileInputDiv">
            <input type="file" id="etsFileInput" class="etsFileInput" @change="this.updateEtsFile($event)">
        </div>
        <div class="resetButton">
            <button class="redButton resetButton" @Click="this.reset()">Reset</button>
        </div>
        <div class="continueButton">
            <button class="greenButton continueButton" @Click="this.continue()">Continue</button>
        </div>
    </div>

</template>

<style>
@import '../assets/base.css';

div.resetButton {
    padding: 2px;
}

button.resetButton {
    position: absolute;
    margin-top: 9px;
    top: 50%;
    left: 0%;
}


div.continueButton {
    padding: 2px;
}

button.continueButton {
    position: absolute;
    margin-top: 9px;
    top: 50%;
    left: 50%;
}


button.addFloor {
    margin-left: 45px;
}

.etsFileInput {
    margin-left: 45px;
}

.etsFileInputDiv {
    margin-top: 12px;
    margin-bottom: 12px;
    margin-left: auto;
    margin-right: auto;
    border-radius: 28px;
    background-color: #eaeaea;
    width: 60%;
    padding: 18px;
}

.floorsCadFilesList {
    list-style: none;
    margin-left: auto;
    margin-right: auto;
}

.floorsListDiv {
    margin-top: 12px;
    margin-bottom: 12px;
    margin-left: auto;
    margin-right: auto;
    border-radius: 28px;
    background-color: #eaeaea;
    width: 60%;
    padding: 18px;
}

.logoSvshiTitlePage {
    display: block;
    margin-top: 18px;
    margin-left: auto;
    margin-right: auto;
    width: 30%;
}

div.titlePage {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    margin-left: auto;
    margin-right: auto;
    margin-top: 20px;
    background-color: #e8c4a2;
    width: 70%;
    border-radius: 28px;
    padding-top: 12px;
    padding-bottom: 80px;
    padding-left: 12px;
    padding-right: 12px;
    height: fit-content;
}

h1.titlePage {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    text-align: center;
    margin-left: auto;
    margin-right: auto;
    margin-top: 20px;
    padding-top: 12px;
    padding-bottom: 12px;
    padding-left: 12px;
    padding-right: 12px;
    font-size: 2.2em;
}

p.titlePage {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    margin-left: auto;
    margin-right: auto;
    margin-top: 18px;
    text-align: center;
    padding-top: 12px;
    padding-bottom: 12px;
    font-size: 1.3em;
}
</style>
