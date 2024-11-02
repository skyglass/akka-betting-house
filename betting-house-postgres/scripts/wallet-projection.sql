\c betting

DROP TABLE IF EXISTS public.wallet_request;

CREATE TABLE IF NOT EXISTS public.wallet_request(
    requestId VARCHAR(255) NOT NULL,
    PRIMARY KEY (requestId));