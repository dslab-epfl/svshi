<script lang="jsx">
import { defineComponent } from 'vue'

export default defineComponent({
    data() {
        return {
        }
    },
    computed: {
    },
    methods: {
        back() {
            console.log("MainPage emits back event")
            this.$emit("back")
        },
        goToDevicesApps() {
            this.$emit("devicesApps")
        },
        goToInstallSimulate() {
            this.$emit("installSimulate")
        },
        refresh() {

        },
        async load() {

        }
    },
    mounted() {
        this.load()
        this.refresh()
    }
})
</script>

<template>
    <div class="mainPage">
        <h1 class="mainPage">SVSHI Webservice</h1>
        <p class="mainPage">Now, the next step is to develop a new app that uses some of your devices.</p>
        <p class="mainPage">To do so,
            you can explore the
            devices available on your installation that are supported by SVSHI.</p>
        <p class="mainPage">You can see your available devices and create a new app under "Devices and apps".</p>
        <p class="mainPage">Once you have some app(s) you can install (and verify) them and try them out on a simulator
            under "Install
            and simulate"</p>

        <table class="buttons">
            <tr>
                <td>
                    <button class="classicButton bigButton" @Click="this.goToDevicesApps()">Devices and apps</button>
                </td>
            </tr>
            <tr>
                <td>
                    <button class="classicButton bigButton" @Click="this.goToInstallSimulate()">Install and
                        simulate</button>
                </td>
            </tr>
        </table>
        <button class="redButton" @Click="this.back()">
            <FontAwesomeIcon icon="arrow-left" /> Back
        </button>
    </div>
</template>

<style>
@import '../assets/base.css';

button.bigButton {
    margin-top: 22px;
    margin-bottom: 22px;
    padding: 45px;
    font-size: 2em;
    margin-left: auto;
    margin-right: auto;
}

td {
    text-align: center;
    vertical-align: middle;
}

.buttons {
    margin-left: auto;
    margin-right: auto;
}

p.mainPage {
    font-family: Verdana, Geneva, Tahoma, sans-serif;
    margin-left: auto;
    margin-right: auto;
    margin-top: 6px;
    text-align: center;
    padding-top: 6px;
    padding-bottom: 12px;
    font-size: 1.3em;
}

h1.mainPage {
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

div.mainPage {
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
