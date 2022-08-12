import { createApp } from 'vue'
import App from './App.vue'
import { library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'

import { faC, faDownload } from "@fortawesome/free-solid-svg-icons";
import { faTrashCan } from "@fortawesome/free-solid-svg-icons";
import { faPlus } from "@fortawesome/free-solid-svg-icons";
import { faUpload } from "@fortawesome/free-solid-svg-icons";
import { faArrowLeft } from "@fortawesome/free-solid-svg-icons";
import { faCheck } from "@fortawesome/free-solid-svg-icons";

library.add(faDownload)
library.add(faTrashCan)
library.add(faPlus)
library.add(faUpload)
library.add(faArrowLeft)
library.add(faCheck)

const app = createApp(App).component("FontAwesomeIcon", FontAwesomeIcon)

app.mount('#app')
