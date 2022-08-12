<script lang="jsx">
import { defineComponent } from 'vue'
import Welcome from "./components/Welcome.vue"
import MainPage from "./components/MainPage.vue"
import DevicesApps from "./components/DevicesApps.vue"
import GenerateApp from "./components/GenerateApp.vue"
import InstallSimulate from './components/InstallSimulate.vue'

export default {
  components: {
    Welcome,
    MainPage,
    DevicesApps,
    GenerateApp,
    InstallSimulate
  },
  data() {
    return {
      defaultBackendServerPort: "4545",
      backendServerAddressPort: "http://localhost:4545",
      backendReachable: false,
      sessionId: "",
      polling: undefined,
      physStructAndSelectedDevicesForGenerateApp: {},
      pageState: "welcome",
      pageStateWelcome: "welcome",
      pageStateSimulator: "simulator",
      pageStateMain: "main",
      pageStateDevicesApps: "devicesApps",
      pageStateInstallSimulate: "installSimulate",
      pageStateGenerateApp: "generateApp",
      colourOrangeSvshi: '#e87000',
    }
  },
  methods: {
    copyObject(obj) {
      return JSON.parse(JSON.stringify(obj))
    },
    setCurrentBackendAddressFromBar() {
      let currentHostname = window.location.hostname;
      let currentProtocol = window.location.protocol;
      let serverAddressIfSameHost = currentProtocol + "//" + currentHostname + ":" + this.defaultBackendServerPort
      this.backendServerAddressPort = serverAddressIfSameHost
    },
    async isBackendReachable() {
      try {
        let req = await fetch(this.backendServerAddressPort + "/version")
        this.backendReachable = req.status == 200
      } catch (error) {
        this.backendReachable = false
      }
      return this.backendReachable
    },
    async hasEtsFile() {
      let url = this.$root.backendServerAddressPort + "/etsFile/"
      let response = await fetch(url, {
        method: 'GET',
      })
      if (response.status != 200) {
        console.log("Error while getting the Ets file! Error was: " + await response.text())
        return false
      } else {
        return true
      }
    },
    async checkEtsFileAndGoToMain() {
      if (await this.hasEtsFile()) {
        this.pageState = this.pageStateMain
      }
    },
    async getOrRefreshSession() {
      let req = await fetch(this.backendServerAddressPort + "/sessionId",
        { method: 'GET' })
      if (req.status != 200) {
        alert("Cannot get a session id")
      } else {
        this.sessionId = await req.text()
        console.log("SessionId = " + this.sessionId)
      }

    },

    // Events listeners ------------------------------------------------------------------------------------
    onWelcomeContinue() {
      this.pageState = this.pageStateMain
    },
    onMainPageBack() {
      this.pageState = this.pageStateWelcome
      console.log("return to welcome")
    },
    onMainPageDevicesApps() {
      this.pageState = this.pageStateDevicesApps
    },
    onMainPageInstallSimulate() {
      this.pageState = this.pageStateInstallSimulate
    },
    onDevicesAppsBack() {
      this.pageState = this.pageStateMain
      console.log("return the main page")
    },
    onDevicesAppsGenerateApp(physStructAndSelectedDevicesInfo) {
      this.physStructAndSelectedDevicesForGenerateApp = this.copyObject(physStructAndSelectedDevicesInfo)
      console.log("DevicesAndApps sends devices to generateApp through App.vue - here is state at App.vue")
      console.log(this.physStructAndSelectedDevicesForGenerateApp)
      this.pageState = this.pageStateGenerateApp
    },
    onGenerateAppBack() {
      this.pageState = this.pageStateDevicesApps
    },
    onGenerateAppMain() {
      this.pageState = this.pageStateMain
    },
    onInstallSimulateBack() {
      this.pageState = this.pageStateMain
    },
    //------------------------------------------------------------------------------------
    refresh() {
      this.setCurrentBackendAddressFromBar()
      this.isBackendReachable()
    },
    startPolling() {
      this.polling = setInterval(() => {
        this.refresh()
      }, 1000)

    },
  },
  async mounted() {
    await this.refresh()
    this.startPolling()
    await this.getOrRefreshSession()
    await this.checkEtsFileAndGoToMain()
  }
}
</script>

<template>
  <div class="backendNotAvailable" v-if="!this.backendReachable">
    <img class="logo_img" src="./assets/svshi_logo.png" width="300">
    <div class="warning_not_connected_box">
      <p>SVSHI API Server unreachable</p>
    </div>
    <p>Please enter the address and port of the machine running SVSHI: </p>
    <input v-model="this.backendServerAddress" placeholder="http://127.0.0.1:4242" />
    <button class="classicButton" @Click="this.checkNewAddress">Connect</button>
  </div>
  <div v-else>
    <div class="Welcome" v-if="this.pageState === this.pageStateWelcome">
      <Welcome @continue="this.onWelcomeContinue()" />
    </div>
    <div class="Main" v-if="this.pageState === this.pageStateMain">
      <MainPage @back="this.onMainPageBack()" @devicesApps="this.onMainPageDevicesApps()"
        @installSimulate="this.onMainPageInstallSimulate()" />
    </div>
    <div class="DevicesApps" v-if="this.pageState === this.pageStateDevicesApps">
      <DevicesApps @back="this.onDevicesAppsBack()" @generateApp="this.onDevicesAppsGenerateApp" />
    </div>
    <div class="GenerateApp" v-if="this.pageState === this.pageStateGenerateApp">
      <GenerateApp :physStructAndSelectedDevices="this.physStructAndSelectedDevicesForGenerateApp"
        @back="this.onGenerateAppBack()" @main="this.onGenerateAppMain()" />
    </div>
    <div class="InstallSimulate" v-if="this.pageState === this.pageStateInstallSimulate">
      <InstallSimulate ref="installAndSimulateComp" @back="this.onInstallSimulateBack()" />
    </div>
    <!-- <div class="Simulator" v-if="this.pageState === this.pageStateSimulator">
      <h1>SIMULATOR PLACEHOLDER</h1>
    </div> -->
  </div>

</template>

<style>
@import './assets/base.css';

.logo_img {
  display: block;
  margin-left: auto;
  margin-right: auto;
}

div.backendNotAvailable {
  margin-top: 120px;
  align-items: center;
  width: fit-content;
  margin-left: auto;
  margin-right: auto;
}

.warning_not_connected_box {
  align-items: center;
  background-color: #dd2323;
  color: white;
  display: flex;
  justify-content: center;
  height: 110px;
  position: relative;
  width: 580px;
  font-size: 30px;
  border-radius: 28px;
}

div.Simulator {
  background-color: aliceblue;
  width: 90%;
}
</style>
