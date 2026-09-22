import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// 入札（注文）を表すクラス
class Bid {
    private String participantId; // 事業者ID (例: 日立、二次請けベンダー、新電力など)
    private boolean isSellOrder;    // true: 売り入札(発電側), false: 買い入札(需要・取次側)
    private double volumeKWh;     // 電力量 (kWh)
    private double priceYen;      // 単価 (円/kWh)

    public Bid(String participantId, boolean isSellOrder, double volumeKWh, double priceYen) {
        this.participantId = participantId;
        this.isSellOrder = isSellOrder;
        this.volumeKWh = volumeKWh;
        this.priceYen = priceYen;
    }

    public String getParticipantId() { return participantId; }
    public boolean isSellOrder() { return isSellOrder; }
    public double getVolumeKWh() { return volumeKWh; }
    public double getPriceYen() { return priceYen; }
    
    public void setVolumeKWh(double volumeKWh) { this.volumeKWh = volumeKWh; }

    @Override
    public String toString() {
        String type = isSellOrder ? "【売り】供給" : "【買い】需要";
        return String.format("[%s] %s: %.1f kWh @ %.2f 円/kWh", type, participantId, volumeKWh, priceYen);
    }
}

// 取引システム（マッチングエンジン）
class TradingSystem {
    private List<Bid> sellBids = new ArrayList<>();
    private List<Bid> buyBids = new ArrayList<>();

    // 入札の受付
    public void submitBid(Bid bid) {
        if (bid.isSellOrder()) {
            sellBids.add(bid);
            // 安い順にソート（売りの優先度高）
            sellBids.sort(Comparator.comparingDouble(Bid::getPriceYen));
        } else {
            buyBids.add(bid);
            // 高い順にソート（買いの優先度高）
            buyBids.sort((b1, b2) -> Double.compare(b2.getPriceYen(), b1.getPriceYen()));
        }
        System.out.println("入札受付: " + bid);
    }

    // マーケットの約定（マッチング）処理
    public void executeMatching() {
        System.out.println("\n--- 【電力取引マッチング処理開始】 ---");
        
        while (!sellBids.isEmpty() && !buyBids.isEmpty()) {
            Bid bestSell = sellBids.get(0);
            Bid bestBuy = buyBids.get(0);

            // 買い手の希望価格が売り手の希望価格以上であれば取引成立
            if (bestBuy.getPriceYen() >= bestSell.getPriceYen()) {
                double tradedVolume = Math.min(bestSell.getVolumeKWh(), bestBuy.getVolumeKWh());
                double settledPrice = bestSell.getPriceYen(); // 約定価格（売り手基準の例）

                System.out.printf("⚡ 取引成立！ [売り: %s] と [買い: %s] -> 数量: %.1f kWh, 約定単価: %.2f 円/kWh%n",
                        bestSell.getParticipantId(), bestBuy.getParticipantId(), tradedVolume, settledPrice);

                // 数量の消化処理
                bestSell.setVolumeKWh(bestSell.getVolumeKWh() - tradedVolume);
                bestBuy.setVolumeKWh(bestBuy.getVolumeKWh() - tradedVolume);

                if (bestSell.getVolumeKWh() <= 0) {
                    sellBids.remove(0);
                }
                if (bestBuy.getVolumeKWh() <= 0) {
                    buyBids.remove(0);
                }
            } else {
                System.out.println("これ以上の価格条件が一致しません。マッチング終了。");
                break;
            }
        }
        System.out.println("--- 【電力取引マッチング処理終了】 ---\n");
    }
}

// 実行用メインクラス
public class PowerTradingSystemMain {
    public static void main(String[] args) {
        TradingSystem system = new TradingSystem();

        // 1. 発電側が入札
        system.submitBid(new Bid("発電所A", true, 1000.0, 15.5));
        system.submitBid(new Bid("発電所B", true, 500.0, 14.8));

        // 2. 需要家・取次事業者（小売側）が入札
        system.submitBid(new Bid("取次事業者X", false, 800.0, 16.0));
        system.submitBid(new Bid("大規模需要家Y", false, 600.0, 15.0));

        // 3. システムによるマッチング・約定実行
        system.executeMatching();
    }
}