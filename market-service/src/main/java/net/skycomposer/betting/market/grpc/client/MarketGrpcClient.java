package net.skycomposer.betting.market.grpc.client;

import com.google.protobuf.Empty;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.skycomposer.betting.common.domain.dto.market.*;
import net.skycomposer.betting.market.grpc.MarketProto;
import net.skycomposer.betting.market.grpc.MarketServiceGrpc;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    public List<MarketData> getAllMarkets() {
        MarketProto.MarketList grpcResponse = stub.getAllMarkets(Empty.getDefaultInstance());
        return grpcResponse.getMarketsList().stream()
                .map(this::createMarketData)
                .collect(Collectors.toList());
    }

    public MarketResponse open(MarketData marketData) {
        MarketProto.MarketData request = createGrpcRequest(marketData);
        MarketProto.Response response = stub.open(request);
        return MarketResponse
                .builder()
                .message(response.getMessage())
                .marketId(marketData.getMarketId())
                .build();
    }

    public MarketResponse update(MarketData marketData) {
        MarketProto.MarketData request = createGrpcRequest(marketData);
        MarketProto.Response response = stub.update(request);
        return MarketResponse
                .builder()
                .message(response.getMessage())
                .marketId(marketData.getMarketId())
                .build();
    }

    public MarketResponse close(CloseMarketRequest request) {
        MarketProto.CloseMarketMessage closeMarketRequest = MarketProto.CloseMarketMessage.newBuilder()
                .setMarketId(request.getMarketId())
                .setResult(request.getResult())
                .build();
        MarketProto.Response response = stub.closeMarket(closeMarketRequest);
        return MarketResponse
                .builder()
                .message(response.getMessage())
                .marketId(request.getMarketId())
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
                .marketId(request.getMarketId())
                .build();
    }

    private MarketProto.MarketData createGrpcRequest(MarketData marketData) {
        MarketProto.MarketData.Builder requestBuilder = MarketProto.MarketData.newBuilder();
         requestBuilder.setMarketId(marketData.getMarketId());
        if (marketData.getFixture() != null) {
            MarketProto.FixtureData fixtureRequest = MarketProto.FixtureData.newBuilder()
                    .setId(marketData.getFixture().getId())
                    .setAwayTeam(marketData.getFixture().getAwayTeam())
                    .setHomeTeam(marketData.getFixture().getHomeTeam())
                    .build();
            requestBuilder.setFixture(fixtureRequest);

        }

        if (marketData.getOdds() != null) {
            MarketProto.OddsData oddsRequest = MarketProto.OddsData.newBuilder()
                    .setTie(marketData.getOdds().getTie())
                    .setWinAway(marketData.getOdds().getWinAway())
                    .setWinHome(marketData.getOdds().getWinHome())
                    .build();
            requestBuilder.setOdds(oddsRequest);
        }

        if (marketData.getResult() != null) {
            requestBuilder.setResult(MarketProto.MarketData.Result.valueOf(marketData.getResult().toString()));
        }

        requestBuilder.setOpensAt(marketData.getOpensAt());

        return requestBuilder.build();
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
                .odds(oddsData)
                .result(grpcResponse.getResult() == null ? null :
                        MarketData.Result.valueOf(grpcResponse.getResult().toString()))
                .opensAt(grpcResponse.getOpensAt())
                .open(grpcResponse.getOpen())
                .build();

        return marketData;
    }


}