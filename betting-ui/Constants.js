export const config = {
  url: {   
    API_BASE_URL: process.env.API_BASE_URL
  },
  keycloak: {
    BASE_URL: process.env.KEYCLOAK_BASE_URL,      
    REALM: "betting-realm", 
    CLIENT_ID: "betting-app"
  } 
}