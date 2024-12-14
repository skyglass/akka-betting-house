export const config = {
  url: {   
    API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL
  },
  keycloak: {
    BASE_URL: process.env.NEXT_PUBLIC_KEYCLOAK_BASE_URL,
    REALM: "betting-realm", 
    CLIENT_ID: "betting-app"
  } 
}