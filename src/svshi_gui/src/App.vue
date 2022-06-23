<script lang="jsx" >
import * as http from "http";
import GenerateApp from './components/GenerateApp.vue'
import GenerationAndCompilation from './components/GenerationAndCompilation.vue'
import Run from './components/Run.vue'
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'
import GeneratePhysicalSystem from './components/GeneratePhysicalSystem.vue';
import { Tabs, Tab } from 'vue3-tabs-component';
import InstalledAppList from './components/InstalledAppList.vue';

let defaultBackendServerAddress = "http://localhost:4242"
let defaultBackendServerPort = "4242"


export default {
  components: {
    "tabs": Tabs,
    "tab": Tab,
    GenerateApp,
    GenerationAndCompilation,
    PulseLoader,
    Run,
    GeneratePhysicalSystem,
    InstalledAppList
  },
  data() {
    return {
      apiReachable: false,
      installedApps: [
        { id: 1, name: "notLoadedYet", deleting: false }
      ],
      isRunning: false,
      uninstallInProgress: false,
      allAppsUninstalling: false,
      backendServerAddress: defaultBackendServerAddress,
      polling: "",
      colourOrangeSvshi: '#e87000',
      allAppsName: "42All"
    }
  },
  methods: {
    async getAppsList() {
      try {
        let res = await fetch(this.backendServerAddress + "/listApps");
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
            let res = await fetch(this.backendServerAddress + "/removeAllApps", requestOptions);
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
            let res = await fetch(this.backendServerAddress + "/removeApp/" + appName, requestOptions);
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
    isNumber(evt) {
      evt = (evt) ? evt : window.event;
      var charCode = (evt.which) ? evt.which : evt.keyCode;
      if ((charCode > 31 && (charCode < 48 || charCode > 57)) && charCode !== 46) {
        evt.preventDefault();;
      } else {
        return true;
      }
    },
    async refresh() {
      await this.checkApiLiveness()
      if (this.apiReachable) {
        this.loadAppsList()
        this.isSvshiRunning()
        if (this.$refs.generateAppComp !== null) {
          this.$refs.generateAppComp.refresh()
        }
        if (this.$refs.generationAndCompilationComp !== null) {
          this.$refs.generationAndCompilationComp.refresh()
        }
        this.goToRunIfRunning()
      } else {
        this.setCurrentBackendAddressFromBar()
        await this.checkApiLiveness()
        if (!this.apiReachable) {
          clearInterval(this.polling)
        }
      }
    },
    async isSvshiRunning() {
      try {
        let res = await fetch(this.backendServerAddress + "/runStatus")
        let responseBody = await res.json()
        this.isRunning = responseBody.status
        return this.isRunning
      }
      catch (error) {
        console.log(error)
        return false
      }
    },
    async goToRunIfRunning() {
      if (this.isRunning) {
      }
    },
    async checkApiLiveness() {
      try {
        let req = await fetch(this.backendServerAddress + "/version")
        let res = await req.json()
        this.apiReachable = res.status
      } catch (error) {
        this.apiReachable = false
      }
      return this.apiReachable

    },
    setCurrentBackendAddressFromBar() {
      let currentHostname = window.location.hostname;
      let currentProtocol = window.location.protocol;
      let serverAddressIfSameHost = currentProtocol + "//" + currentHostname + ":" + defaultBackendServerPort
      this.backendServerAddress = serverAddressIfSameHost
    },
    startPolling() {
      this.polling = setInterval(() => {
        this.refresh()
      }, 1000)

    },
    async checkNewAddress() {
      if (await this.checkApiLiveness()) {
        this.startPolling()
      } else {
        alert("Cannot connect to the given address!")
      }
    },
    scrollToTop() {
      window.scrollTo(0, 0);
    },
    tabChanged(selectedTab) {
      if (this.$refs.generationAndCompilationComp !== null) {
        this.$refs.generationAndCompilationComp.installationStep = this.$refs.generationAndCompilationComp.stepWelcome
      }
      this.scrollToTop()
    }
  },
  mounted() {
    this.refresh()
    this.startPolling()
  }
}
</script>

<template>
  <img class="logo_img" src="./assets/logo.png" width="300">
  <div v-if="!this.apiReachable">
    <div class="warning_not_connected_box">
      <p>SVSHI API Server unreachable</p>
    </div>
    <p>Please enter the address and port of the machine running SVSHI: </p>
    <input v-model="this.backendServerAddress" placeholder="http://127.0.0.1:4242" />
    <button class="classicButton" @Click="this.checkNewAddress">Connect</button>
  </div>
  <div v-else>
    <tabs @changed="tabChanged">
      <tab name="Apps">
        <GenerationAndCompilation ref="generationAndCompilationComp" />
      </tab>
      <tab name="Generate app" :is-disabled="this.isRunning">
        <GenerateApp ref="generateAppComp" />
      </tab>
      <tab name="Run">
        <Run ref="runComp" />
      </tab>
      <tab name="Physical System simulator">
        <GeneratePhysicalSystem ref="generatePhysicalSystemComp" />
      </tab>
    </tabs>
  </div>

</template>

<style>
@import './assets/base.css';

ul.appsList {
  padding-top: 16px;
  padding-bottom: 36px;
  padding-left: 32px;
  list-style-type: none;
}

.uninstallAll {
  margin-left: 52px;
}

h2.installedAppTitle {
  margin-left: 4;
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
}

.logo_img {
  display: block;
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

#app {
  max-width: 1600px;
  margin: 0 auto;
  padding: 2rem;

  font-weight: normal;
}

header {
  line-height: 1.5;
}

.logo {
  display: block;
  margin: 0 auto 2rem;
}

a,
.green {
  text-decoration: none;
  color: hsla(160, 100%, 37%, 1);
  transition: 0.4s;
}


@media (min-width: 1024px) {
  body {
    display: flex;
    place-items: center;
  }

  #app {
    display: grid;
    /* grid-template-columns: 1fr 2fr; */
    padding: 0 2rem;
  }

  header {
    display: flex;
    place-items: center;
    padding-right: calc(var(--section-gap) / 2);
  }

  header .wrapper {
    display: flex;
    place-items: flex-start;
    flex-wrap: wrap;
  }

  .logo {
    margin: 0 2rem 0 0;
  }
}

.tabs-component {
  margin: auto auto;
  width: 90vw;
}

.tabs-component-tabs {
  border: solid 1px #ddd;
  border-radius: 6px;
  margin-bottom: 5px;
}

.tabs-component-tabs {
  border: 0;
  align-items: stretch;
  display: flex;
  justify-content: flex-start;
  margin-bottom: -1px;
}

.tabs-component-tab {
  color: #999;
  font-size: 14px;
  font-weight: 600;
  margin-right: 0;
  list-style: none;
}

.tabs-component-tab:not(:last-child) {
  border-bottom: dotted 1px #ddd;
}

.tabs-component-tab:hover {
  color: #666;
}

.tabs-component-tab.is-active {
  color: #000;
}

.tabs-component-tab.is-disabled * {
  color: #cdcdcd;
  cursor: not-allowed !important;
}

.tabs-component-tab {
  background-color: #fff;
  border: solid 1px #ddd;
  border-radius: 3px 3px 0 0;
  margin-right: .5em;
  transform: translateY(2px);
  transition: transform .3s ease;
}

.tabs-component-tab.is-active {
  border-bottom: solid 1px #fff;
  z-index: 2;
  transform: translateY(0);
}


.tabs-component-tab-a {
  align-items: center;
  color: inherit;
  display: flex;
  padding: .75em 1em;
  text-decoration: none;
}

.tabs-component-panels {
  padding: 4em 0;
}

.tabs-component-panels {
  background-color: #fff;
  border: solid 1px #ddd;
  border-radius: 0 6px 6px 6px;
  box-shadow: 0 0 10px rgba(0, 0, 0, .05);
  padding: 4em 2em;
}
</style>
