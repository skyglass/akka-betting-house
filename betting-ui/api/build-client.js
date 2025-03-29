import axios from 'axios';
import { config } from '../Constants'

export default ({ req = {}, currentUser = {} }) => {
  if (typeof window === 'undefined') {

    return axios.create({
      baseURL: config.url.BASE_URL,
      headers: {
        ...req.headers,
        Authorization: `Bearer ${currentUser.token}`
      }
    })
  } else {
    // We must be on the browser
    return axios.create({
      baseUrl: '/',
      headers: {
        ...req.headers,
        Authorization: `Bearer ${currentUser.token}`, // Use token from Keycloak
      },
    });
  }
};
