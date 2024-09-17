package net.skycomposer.betting.market.grpc.client;

import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.skycomposer.betting.common.domain.dto.market.*;
import net.skycomposer.betting.market.grpc.MarketProto;
import net.skycomposer.betting.market.grpc.MarketServiceGrpc;
import org.springframework.stereotype.Service;

@Service
public class MarketGrpcClient {

    @GrpcClient("market-grpc-server")
    private MarketServiceGrpc.MarketServiceBlockingStub stub;

    public MarketData getState(String marketId) {
        MarketProto.MarketId marketIdRequest = MarketProto.MarketId.newBuilder()
                .setMarketId(marketId)
                .build();
        MarketProto.MarketData grpcResponse = stub.getState(marketIdRequest);
        return createMarketData(grpcResponse);
    }

    public MarketResponse open(MarketData marketData) {
        MarketProto.MarketData request = createGrpcRequest(marketData);
        MarketProto.Response response = stub.open(request);
        return MarketResponse
                .builder()
                .message(response.getMessage())
                .build();
    }

    public MarketResponse update(MarketData marketData) {
        MarketProto.MarketData request = createGrpcRequest(marketData);
        MarketProto.Response response = stub.update(request);
        return MarketResponse
                .builder()
                .message(response.getMessage())
                .build();
    }

    public MarketResponse close(CloseMarketRequest request) {
        MarketProto.MarketId marketIdRequest = MarketProto.MarketId.newBuilder()
                .setMarketId(request.getMarketId())
                .build();
        MarketProto.Response response = stub.closeMarket(marketIdRequest);
        return MarketResponse
                .builder()
                .message(response.getMessage())
                .build();
    }

    public MarketResponse cancel(CancelMarketRequest request) {
        MarketProto.CancelMarket grpcRequest = MarketProto.CancelMarket.newBuilder()
                .setMarketId(request.getMarketId())
                .setReason(request.getReason())
                .build();
        MarketProto.Response response = stub.cancel(grpcRequest);
        return MarketResponse
                .builder()
                .message(response.getMessage())
                .build();
    }

    private MarketProto.MarketData createGrpcRequest(MarketData marketData) {
        MarketProto.FixtureData fixtureRequest = MarketProto.FixtureData.newBuilder()
                .setId(marketData.getFixture().getId())
                .setAwayTeam(marketData.getFixture().getAwayTeam())
                .setHomeTeam(marketData.getFixture().getHomeTeam())
                .build();
        MarketProto.OddsData oddsRequest = MarketProto.OddsData.newBuilder()
                .setTie(marketData.getOdds().getTie())
                .setWinAway(marketData.getOdds().getWinAway())
                .setWinHome(marketData.getOdds().getWinHome())
                .build();
        MarketProto.MarketData request = MarketProto.MarketData.newBuilder()
                .setMarketId(marketData.getMarketId())
                .setFixture(fixtureRequest)
                .setOpensAt(marketData.getOpensAt())
                .setOdds(oddsRequest)
                .setResult(MarketProto.MarketData.Result.valueOf(marketData.getResult().toString()))
                .build();

        return request;
    }

    private MarketData createMarketData(MarketProto.MarketData grpcResponse) {
        FixtureData fixtureData = FixtureData.builder()
                .id(grpcResponse.getFixture().getId())
                .awayTeam(grpcResponse.getFixture().getAwayTeam())
                .homeTeam(grpcResponse.getFixture().getHomeTeam())
                .build();
        OddsData oddsData = OddsData.builder()
                .tie(grpcResponse.getOdds().getTie())
                .winAway(grpcResponse.getOdds().getWinAway())
                .winHome(grpcResponse.getOdds().getWinHome())
                .build();

        MarketData marketData = MarketData.builder()
                .marketId(grpcResponse.getMarketId())
                .fixture(fixtureData)
                .opensAt(grpcResponse.getOpensAt())
                .odds(oddsData)
                .result(MarketData.Result.valueOf(grpcResponse.getResult().toString()))
                .build();

        return marketData;
    }


}