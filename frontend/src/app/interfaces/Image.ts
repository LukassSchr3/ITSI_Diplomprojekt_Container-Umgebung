import {Instance} from './Instance';

export interface Image {
  ID: bigint;
  Name: string;
  URL: string;
  Instances?: Instance[];
}
