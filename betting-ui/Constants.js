export const config = {
  url: {   
    BASE_URL: process.env.NEXT_PUBLIC_BASE_URL
  },
  keycloak: {
    BASE_URL: process.env.NEXT_PUBLIC_KEYCLOAK_BASE_URL,
    REALM: "betting-realm", 
    CLIENT_ID: "betting-app"
  } 
}