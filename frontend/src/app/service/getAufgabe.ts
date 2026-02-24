import axios from 'axios';
import {Image} from '../models/Image';

const baseURL: string = 'https://api.example.com/aufgaben';



export class getAufgaben {

  //alle Aufgaben eines Users holen
  async getAllImagesForUser(userID: bigint) {
      return axios.get<Image[]>(`${baseURL}/user/${userID}/images`);
  }
}

