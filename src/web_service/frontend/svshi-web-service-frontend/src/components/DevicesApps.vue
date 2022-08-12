<script lang="jsx">
import { defineComponent } from 'vue'
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'

export default defineComponent({
    component: {
        PulseLoader
    },
    data() {
        return {
            physicalStructure: {},
            deviceMappings: [],
            selectedDevicesInfo: [],
            loading: false,
            protoDevicesDescription: "- switch: actuator of boolean type, e.g., light switch, socket on/off actuator, ...\n\
- binarySensor: sensor of boolean type, e.g., a push button, a presence detector, ...\n\
- temperatureSensor: sensor of type °C or °F\n\
- co2Sensor: sensor of type ppm\n\
- humditySensor: sensor of type % for humidity\n\
- dimmerSensor: sensor of type DPT5-1 so dimming value, e.g. (0..100%) or blinds position (0..100%)\n\
- dimmerActuator: actuator type DPT5-1 so dimming value, e.g. (0..100%) or blinds position (0..100%)"
        };
    },
    computed: {
        filteredDevices: function () {
            let filtered = this.copyObject(this.deviceMappings)
            return filtered.filter(m => {
                m.supportedDeviceMappingNodes = m.supportedDeviceMappingNodes.filter(n => {
                    n.supportedDeviceMappings = n.supportedDeviceMappings.filter(dm => {
                        return this.selectedDevicesInfo.some(x => x.physicalCommObjectId === dm.physicalCommObjectId && x.supportedDeviceName === dm.supportedDeviceName)
                    })
                    return n.supportedDeviceMappings.length > 0
                })
                return m.supportedDeviceMappingNodes.length > 0
            })
        },
    },
    methods: {
        copyObject(obj) {
            return JSON.parse(JSON.stringify(obj))
        },
        back() {
            this.$emit("back")
        },
        generateApp() {
            this.$emit("generateApp", { "physicalStructureJson": this.physicalStructure, "selectedDevices": this.copyObject(this.filteredDevices) })
        },
        async getDeviceMappings() {
            let url = this.$root.backendServerAddressPort + "/getDeviceMappings";
            this.loading = true;
            let response = await fetch(url, {
                method: "GET",
            });
            this.loading = false;
            if (response.status != 200) {
                alert("Cannot get the mappings! Internal problem when communicating with SVSHI!")
                console.log("Cannot get the deviceMappings!");
                return null;
            }
            else {
                let received = await response.json();
                this.deviceMappings = received.deviceMappings;
                this.physicalStructure = received.physicalStructureJson;
            }
        },
        getDeviceName(physAddr) {
            let opt = this.physicalStructure.deviceInstances.find(d => d.address == physAddr);
            if (typeof opt !== "undefined") {
                return opt.name;
            }
            else {
                return "";
            }
        },
        selectDevice(supportedDeviceMapping) {
            this.selectedDevicesInfo.push(supportedDeviceMapping)
        },
        unselectDevice(supportedDeviceMapping) {
            this.selectedDevicesInfo.pop(supportedDeviceMapping)
        },
        isDeviceSelected(supportedDeviceMapping) {
            return this.selectedDevicesInfo.some(x => x.physicalCommObjectId === supportedDeviceMapping.physicalCommObjectId && x.supportedDeviceName === supportedDeviceMapping.supportedDeviceName)
        },
        showProtoDevicesDescription() {
            alert(this.protoDevicesDescription)
        },
        refresh() {
        },
        async load() {
            this.getDeviceMappings();
        }
    },
    mounted() {
        this.load();
        this.refresh();
    },
    components: { PulseLoader }
})
</script>

<template>
    <div class="devicesApps">
        <button class="classicButton" @Click="this.back()">
            <FontAwesomeIcon icon="arrow-left" /> Back
        </button>
        <h1 class="devicesApps">Devices and new apps</h1>


        <PulseLoader id="loader" v-if="loading" :color="this.$root.colourOrangeSvshi" />
        <p class="devicesApps">On the left, you have all your physical devices and, inside each of them, the list of sub
            devices supported
            by SVSHI that they offer.</p>
        <p class="devicesApps">To create a new SVSHI app, select the sub devices your app will use and click on
            "Generate a new App" once
            you're ready.</p>
        <p class="devicesApps">A "prototypical device" is a generic sub device that is supported by SVSHI. Each "real"
            (also called "physical") device can contain multiple functions that are equivalent to a prototypical device.
        </p>
        <p class="devicesApps">To have a description of each prototypical device, please click here:</p>
        <button class="classicButton" @Click="this.showProtoDevicesDescription()">Proto. devices description</button>
        <div id="gridDevicesApps">
            <div id="column1">
                <h2 class="devicesApps">Available prototypical devices</h2>
                <template v-for="mapping in this.deviceMappings">
                    <div class="deviceMappingDiv">
                        <h3>{{ getDeviceName(mapping.physicalAddress) }} - {{ mapping.physicalAddress }}</h3>


                        <template v-for="node in mapping.supportedDeviceMappingNodes">
                            <div class="mappingNodeDiv">
                                <h4>{{ node.name }}</h4>
                                <template v-for="deviceMapping in node.supportedDeviceMappings">
                                    <div v-if="this.isDeviceSelected(deviceMapping)"
                                        class="selected mappingProtoDeviceDiv unselectedColumnDeviceMapping"
                                        @Click="this.unselectDevice(deviceMapping)">
                                        <p>{{ deviceMapping.name }}</p>
                                        <p class="supportedDeviceType"
                                            v-if="'humanReadableInfo' in deviceMapping && deviceMapping.humanReadableInfo !== ''">
                                            Type: {{ deviceMapping.supportedDeviceName }}
                                            ({{ deviceMapping.humanReadableInfo }})</p>
                                        <p class="supportedDeviceType" v-else>Type: {{
                                                deviceMapping.supportedDeviceName
                                        }}</p>

                                    </div>
                                    <div v-else class="mappingProtoDeviceDiv unselectedColumnDeviceMapping"
                                        @Click="this.selectDevice(deviceMapping)">
                                        <p>{{ deviceMapping.name }}</p>
                                        <p class="supportedDeviceType"
                                            v-if="'humanReadableInfo' in deviceMapping && deviceMapping.humanReadableInfo !== ''">
                                            Type: {{ deviceMapping.supportedDeviceName }}
                                            ({{ deviceMapping.humanReadableInfo }})</p>
                                        <p class="supportedDeviceType" v-else>Type: {{
                                                deviceMapping.supportedDeviceName
                                        }}</p>
                                    </div>

                                </template>
                            </div>
                        </template>
                    </div>

                </template>
            </div>
            <div id="column2">
                <button class="classicButton" @Click="this.generateApp()">Generate a new App</button>
                <h2 class="devicesApps">Recap</h2>
                <template v-for="mapping in     this.filteredDevices">
                    <div class="deviceMappingDiv">
                        <h3>{{ getDeviceName(mapping.physicalAddress) }} - {{ mapping.physicalAddress }}</h3>


                        <template v-for="node in     mapping.supportedDeviceMappingNodes">
                            <div class="mappingNodeDiv">
                                <h4>{{ node.name }}</h4>
                                <template v-for="deviceMapping in     node.supportedDeviceMappings">
                                    <div class="mappingProtoDeviceDiv selectedColumnDeviceMapping"
                                        @Click="this.unselectDevice(deviceMapping)">
                                        <p>{{ deviceMapping.name }}</p>
                                        <p class="supportedDeviceType"
                                            v-if="'humanReadableInfo' in deviceMapping && deviceMapping.humanReadableInfo !== ''">
                                            Type: {{ deviceMapping.supportedDeviceName }}
                                            ({{ deviceMapping.humanReadableInfo }})</p>
                                        <p class="supportedDeviceType" v-else>Type: {{
                                                deviceMapping.supportedDeviceName
                                        }}</p>

                                    </div>
                                </template>
                            </div>
                        </template>
                    </div>

                </template>
            </div>

        </div>

    </div>
</template>

<style>
@import '../assets/base.css';

#column1 {
    margin-right: 12px;
}


#column2 {
    margin-left: 12px;
}

p.supportedDeviceType {
    font-weight: bold;
}

#gridDevicesApps {
    display: grid;
    grid-template-columns: 60% 40%;
}

#loader {
    margin-top: 28px;
    margin-left: auto;
    margin-right: auto;
    width: fit-content;
}


div.unselectedColumnDeviceMapping:hover {
    background: linear-gradient(to bottom, #83d164 5%, #5a9542 100%);
    cursor: pointer;
}

div.selectedColumnDeviceMapping:hover {
    color: #ffffff;
    background: linear-gradient(to bottom, #ff2626 5%, #bb0a0a 100%);
    cursor: pointer;
}

div.mappingProtoDeviceDiv.selected {
    background: linear-gradient(to bottom, #83d164 5%, #5a9542 100%);
}


div.mappingProtoDeviceDiv {
    background: linear-gradient(to bottom, #f7f7f7 5%, #dddddd 100%);
    border-radius: 28px;
    padding-left: 12px;
    padding-right: 12px;
    padding-top: 6px;
    padding-bottom: 6px;
    margin-top: 6px;
    margin-bottom: 6px;
}

div.mappingNodeDiv {
    background-color: rgb(218, 218, 218);
    border-radius: 28px;
    padding-left: 12px;
    padding-right: 12px;
    padding-top: 6px;
    padding-bottom: 6px;
    margin-top: 6px;
    margin-bottom: 6px;
}

div.deviceMappingDiv {
    background-color: aliceblue;
    border-radius: 28px;
    padding-left: 12px;
    padding-right: 12px;
    padding-top: 6px;
    padding-bottom: 6px;
    margin-top: 6px;
    margin-bottom: 6px;
}

div.mappingGrid {
    display: grid;
    grid-template-columns: 50% 50%;
}


h1.devicesApps {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    text-align: center;
    margin-left: auto;
    margin-right: auto;
    margin-top: 8px;
    padding-top: 12px;
    padding-bottom: 12px;
    padding-left: 12px;
    padding-right: 12px;
    font-size: 2.2em;
}

p.devicesApps {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    margin-left: auto;
    margin-right: auto;
    margin-top: 6px;
    text-align: left;
    padding-top: 6px;
    padding-bottom: 12px;
    font-size: 1.3em;
}


h2.devicesApps {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    text-align: left;
    margin-top: 8px;
    padding-top: 12px;
    padding-bottom: 12px;
    padding-left: 12px;
    padding-right: 12px;
    font-size: 2.2em;
}

div.devicesApps {
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
</style>
