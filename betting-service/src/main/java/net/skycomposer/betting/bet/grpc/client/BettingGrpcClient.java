package net.skycomposer.betting.bet.grpc.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import net.devh.boot.grpc.client.inject.GrpcClient;
import net.skycomposer.betting.bet.grpc.BetProto;
import net.skycomposer.betting.bet.grpc.BetServiceGrpc;
import net.skycomposer.betting.common.domain.dto.betting.*;
import net.skycomposer.betting.projection.proto.BetProjectionProto;
import net.skycomposer.betting.projection.proto.BetProjectionServiceGrpc;

@Service
public class BettingGrpcClient {

    @GrpcClient("betting-grpc-server")
    BetServiceGrpc.BetServiceBlockingStub stub;

    @GrpcClient("betting-grpc-projection-server")
    BetProjectionServiceGrpc.BetProjectionServiceBlockingStub projectionStub;

    public BetData getState(String betId) {
        BetProto.BetId betIdRequest = BetProto.BetId.newBuilder()
                .setBetId(betId)
                .build();
        BetProto.Bet grpcResponse = stub.getState(betIdRequest);
        return createBetData(grpcResponse);
    }

    public SumStakesData getBetByMarket(String marketId) {
        BetProjectionProto.MarketIdsBet marketIdRequest = BetProjectionProto.MarketIdsBet.newBuilder()
                .setMarketId(marketId)
                .build();
        BetProjectionProto.SumStakes grpcResponse = projectionStub.getBetByMarket(marketIdRequest);
        return createSumStakesData(grpcResponse);
    }

    public BetDataList getBetsForMarket(String marketId) {
        BetProjectionProto.MarketIdsBet marketIdRequest = BetProjectionProto.MarketIdsBet.newBuilder()
                .setMarketId(marketId)
                .build();
        BetProjectionProto.BetDataList grpcResponse = projectionStub.getBetsForMarket(marketIdRequest);
        return createBetDataList(grpcResponse);
    }

    public BetDataList getBetsForPlayer(String walletId) {
        BetProjectionProto.WalletIdsBet walletIdRequest = BetProjectionProto.WalletIdsBet.newBuilder()
                .setWalletId(walletId)
                .build();
        BetProjectionProto.BetDataList grpcResponse = projectionStub.getBetsForPlayer(walletIdRequest);
        return createBetDataList(grpcResponse);
    }

    public BetResponse open(BetData betData) {
        BetProto.Bet request = createGrpcRequest(betData);
        BetProto.BetResponse response = stub.open(request);
        return BetResponse
                .builder()
                .message(response.getMessage())
                .betId(betData.getBetId())
                .build();
    }

    public BetResponse cancel(CancelBetRequest request) {
        BetProto.CancelMessage cancelMessage = BetProto.CancelMessage.newBuilder()
                .setBetId(request.getBetId())
                .setReason(request.getReason())
                .build();
        BetProto.BetResponse response = stub.cancel(cancelMessage);
        return BetResponse
                .builder()
                .message(response.getMessage())
                .betId(request.getBetId())
                .build();
    }

    private BetProto.Bet createGrpcRequest(BetData betData) {
        BetProto.Bet request = BetProto.Bet.newBuilder()
                .setBetId(betData.getBetId())
                .setMarketId(betData.getMarketId())
                .setMarketName(betData.getMarketName())
                .setWalletId(betData.getWalletId())
                .setOdds(betData.getOdds())
                .setStake(betData.getStake())
                .setResult(betData.getResult())
                .build();

        return request;
    }

    private BetData createBetData(BetProto.Bet grpcResponse) {
        BetData betData = BetData.builder()
                .betId(grpcResponse.getBetId())
                .marketId(grpcResponse.getMarketId())
                .marketName(grpcResponse.getMarketName())
                .walletId(grpcResponse.getWalletId())
                .odds(grpcResponse.getOdds())
                .stake(grpcResponse.getStake())
                .result(grpcResponse.getResult())
                .marketConfirmed(grpcResponse.getMarketConfirmed())
                .fundsConfirmed(grpcResponse.getFundsConfirmed())
                .build();

        return betData;
    }

    private SumStakesData createSumStakesData(BetProjectionProto.SumStakes grpcResponse) {
        List<SumStakeData> sumStakes = new ArrayList<>();
        for (BetProjectionProto.SumStake sumStake: grpcResponse.getSumstakesList()) {
            SumStakeData sumStakeData = SumStakeData.builder()
                    .total(sumStake.getTotal())
                    .result(sumStake.getResult())
                    .build();
            sumStakes.add(sumStakeData);
        }
        SumStakesData sumStakesData = SumStakesData.builder()
                .sumStakes(sumStakes)
                .build();

        return sumStakesData;
    }

    private BetDataList createBetDataList(BetProjectionProto.BetDataList grpcResponse) {
        List<BetData> betDataList = new ArrayList<>();
        for (BetProjectionProto.BetData betDataGrpc: grpcResponse.getBetDatasList()) {
            BetData betData = BetData.builder()
                    .betId(betDataGrpc.getBetId())
                    .walletId(betDataGrpc.getWalletId())
                    .marketId(betDataGrpc.getMarketId())
                    .marketName(betDataGrpc.getMarketName())
                    .odds(betDataGrpc.getOdds())
                    .stake(betDataGrpc.getStake())
                    .result(betDataGrpc.getResult())
                    .build();
            betDataList.add(betData);
        }
        BetDataList result = BetDataList.builder()
                .betDataList(betDataList)
                .build();

        return result;
    }


}