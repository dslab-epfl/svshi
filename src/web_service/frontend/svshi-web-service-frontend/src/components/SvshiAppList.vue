<script lang="jsx" >
import PulseLoader from 'vue-spinner/src/PulseLoader.vue'


export default {
    components: {
        PulseLoader,
    },
    data() {
        return {
            apps: [],
        }
    },
    computed: {

    },
    methods: {
        sleep(ms) {
            return new Promise(
                resolve => setTimeout(resolve, ms)
            )
        },
        async getApps() {
            try {
                let res = await fetch(this.$root.backendServerAddressPort + "/svshi/applications/installed/names");
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
    <div class="appToBeInstalled">
        <ul class="appsList">
            <li v-if="this.apps.length > 0" v-for='app in apps' :key="app.id">
                <table>
                    <tr>
                        <td>
                            <p class="appName">{{ app.id + 1 }}</p>
                        </td>
                        <td>
                            <p class="appName">{{ app.name }}</p>
                        </td>
                    </tr>
                </table>
            </li>
            <li v-else>
                <p class="appName">No apps on SVSHI, please install a set of apps from above.</p>
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

.appToBeInstalled {
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

h3.appToBeInstalledTitle {
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
