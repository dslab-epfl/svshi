<script lang="jsx">

let id = 0

export default {

    data() {
        return {
            physStruct: {
                deviceInstances: []
            },
            defaultPhysStruct: {
                deviceInstances: []
            },
            defaultDeviceInstance: {
                name: "dev1",
                address: "1.1.1",
                nodes: []
            },
            defaultNode: {
                name: "default",
                comObjects: []
            },
            defaultComObject: {
                name: "",
                datatype: "",
                ioType: "",
                id: 42
            },
            availableDpts: ["notLoadedYet"],
            availableIoTypes: ["in", "out", "in/out"]
        }
    },
    methods: {
        async loadAvailableDpts() {
            try {
                let res = await fetch(this.$root.backendServerAddress + "/availableDpts");
                let responseBody = await res.json();
                this.availableDpts = responseBody.output
            } catch (error) {
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
        getNewId: function () {
            return Math.floor(Math.random() * 100000000)
        },
        addNewComObjectTo(deviceInstance) {
            let newComObject = this.copyObject(this.defaultComObject)
            newComObject["id"] = this.getNewId()
            deviceInstance.nodes[0].comObjects.push(newComObject)
        },
        addNewDevice: function () {
            this.physStruct.deviceInstances.push({ id: id++, name: '', address: '', nodes: [this.copyObject(this.defaultNode)] })
        },
        copyObject(obj) {
            return JSON.parse(JSON.stringify(obj))
        },
        isValidPhysAddr(addr) {
            let regex = /^[1-9][0-9]?\.[1-9][0-9]?\.[1-9][0-9]?[0-9]?$/
            let find = addr.match(regex)
            if (find === null) {
                return false
            }
            let numbers = addr.split(".")
            let nArea = numbers[0]
            let nLine = numbers[1]
            let nDevice = numbers[2]
            if (nArea > 15 || nLine > 15 || nDevice > 255) {
                return false
            }
            return true
        },
        validateStructure() {
            let physAddr = []
            for (let i = 0; i < this.physStruct.deviceInstances.length; i++) {
                let d = this.physStruct.deviceInstances[i]
                if (d.name === "") {
                    alert("The physical system is not valid!\nA device cannot have an empty name!")
                    return false
                }
                if (!this.isValidPhysAddr(d.address)) {
                    alert("The physical system is not valid!\nThe physical address of the device with name '" + d.name + "' has a wrong format. The correct format is '[1-15].[1.15].[1-255]'")
                    return false
                }
                if (physAddr.includes(d.address)) {
                    alert("The physical system is not valid!\nThe physical address of the device with name '" + d.name + "' is already used by a previous device!")
                    return false
                }
                physAddr.push(d.address)
                let comObjects = d.nodes[0].comObjects
                for (let j = 0; j < comObjects.length; j++) {
                    let comObject = comObjects[j]
                    if (comObject.name === "") {
                        alert("The physical system is not valid!\nA comObject cannot have an empty name!")
                        return false
                    }
                    if (!this.availableIoTypes.includes(comObject.ioType)) {
                        alert("The physical system is not valid!\nThe comObject with name '" + comObject.name + "' of the device with name '" + d.name + "' has an invalid IO Type: '" + comObject.ioType + "'!\nValid IO Types are: " + this.availableIoTypes)
                        return false
                    }
                    if (!this.availableDpts.includes(comObject.datatype)) {
                        alert("The physical system is not valid!\nThe comObject with name '" + comObject.name + "' of the device with name '" + d.name + "' has an invalid KNX datatype: '" + comObject.datatype + "'!\nValid KNX datatypes are: " + this.availableDpts)
                        return false
                    }
                }
            }
            return true
        },
        downloadCurrentPhysStruct() {
            if (this.validateStructure()) {
                try {
                    let filename = "physical_system_structure.json"
                    const data = JSON.stringify(this.physStruct, null, 2)
                    const blob = new Blob([data], { type: 'text/plain' })

                    let elm = document.createElement('a')
                    elm.href = URL.createObjectURL(blob)
                    elm.setAttribute('download', filename) // SET ELEMENT CREATED 'ATTRIBUTE' TO DOWNLOAD, FILENAME PARAM AUTOMATICALLY
                    elm.click()
                }
                catch (error) {
                    console.log(error)
                }
            }
        },
        async refresh() {

        }
    },
    mounted() {
        this.loadAvailableDpts()
    }
}
</script>

<template>
    <h2>Generate a new physical system for the simlator:</h2>
    <table>
        <tr>
            <td>Device instances:</td>
            <td>
                <ul style="list-style-type:none;">
                    <li v-for='dev in this.physStruct.deviceInstances' :key="dev.id">
                        <table>
                            <tr>
                                <input v-model="dev.name" placeholder="device name" />
                                <input v-model="dev.address" placeholder="device physical address" />
                                <button class="redButton"
                                    @Click="this.physStruct.deviceInstances = this.physStruct.deviceInstances.filter(d => d.id !== dev.id)">X</button>
                            </tr>
                            <tr>
                                <ul>
                                    <li v-for='comObject in dev.nodes[0].comObjects' :key="comObject.id">
                                        <input v-model="comObject.name" placeholder="comObject name" />
                                        <select v-model="comObject.ioType">
                                            <option disabled value="">IO Type: Please select one</option>
                                            <option v-for="ioType in this.availableIoTypes" :value="ioType">
                                                {{ ioType }}
                                            </option>
                                        </select>
                                        <select v-model="comObject.datatype">
                                            <option disabled value="">KNX DPT: Please select one</option>
                                            <option v-for="dpt in this.availableDpts" :value="dpt">
                                                {{ dpt }}
                                            </option>
                                        </select>
                                        <button class="redButton"
                                            @Click="dev.nodes[0].comObjects = dev.nodes[0].comObjects.filter(c => c.id !== comObject.id)">X</button>
                                    </li>
                                </ul>
                            </tr>
                            <tr>
                                <button class="classicButton" @Click="this.addNewComObjectTo(dev)">Add
                                    comObject</button>
                            </tr>
                        </table>

                    </li>
                </ul>
                <button class="classicButton" @Click="this.addNewDevice">Add
                    device</button>
            </td>
        </tr>
    </table>
    <table>
        <tr>
            <td>
                <button class="greenButton" @Click="this.downloadCurrentPhysStruct">Download physical system
                    structure</button>
            </td>
            <td>
                <button class="redButton" @Click="this.physStruct = { deviceInstances: [] }">Reset</button>
            </td>
        </tr>
    </table>


</template>

<style>
@import '../assets/base.css';
</style>
