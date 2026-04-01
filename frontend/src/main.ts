import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import {
    faPen,faTrash,faChessKnight,faCloudArrowUp, faChessPawn,faRotate,faArrowRotateLeft,faChessBoard
} from '@fortawesome/free-solid-svg-icons'

library.add(
    faPen,faTrash,faChessKnight,faCloudArrowUp, faChessPawn,faRotate,faArrowRotateLeft,faChessBoard
)

const app = createApp(App)
app.component('FontAwesomeIcon', FontAwesomeIcon)
app.mount('#app')
