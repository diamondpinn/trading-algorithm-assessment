import React, { useEffect, useRef, useCallback } from "react";
import { MarketDepthRow } from "../market-depth/useMarketDepthData";
import "./MarketTable.css";

interface MarketTableProps {
  data: MarketDepthRow[];
}

const MarketTable: React.FC<MarketTableProps> = ({ data }) => {
  const previousPrices = useRef<{
    [key: string]: { bid: number; offer: number };
  }>({});

  const getDirectionIcon = useCallback((direction: string) => {
    switch (direction) {
      case "up":
        return "↑";
      case "down":
        return "↓";
      default:
        return "-";
    }
  }, []);

  useEffect(() => {
    data.forEach((item) => {
      previousPrices.current[item.symbolLevel] = {
        bid: item.bid,
        offer: item.offer,
      };
    });
  }, [data]);

  return (
    <div>
      <table>
        <thead>
          <tr>
            <th colSpan={3} style={{ textAlign: "left" }}>
              BID
            </th>
            <th colSpan={2} style={{ textAlign: "right" }}>
              ASK
            </th>
          </tr>
          <tr>
            <th>S/N</th>
            <th>Quantity</th>
            <th>Price</th>
            <th>Price</th>
            <th>Quantity</th>
          </tr>
        </thead>
        <tbody>
          {data.map((item, index) => {
            const prevBid =
              previousPrices.current[item.symbolLevel]?.bid ?? item.bid;
            const prevOffer =
              previousPrices.current[item.symbolLevel]?.offer ?? item.offer;

            // Calculate percentage changes
            const bidChange = ((item.bid - prevBid) / prevBid) * 100;
            const offerChange = ((item.offer - prevOffer) / prevOffer) * 100;

            const priceDirection =
              item.bid > prevBid
                ? "up"
                : item.bid < prevBid
                ? "down"
                : "neutral";
            const offerDirection =
              item.offer > prevOffer
                ? "up"
                : item.offer < prevOffer
                ? "down"
                : "neutral";

            // Arrow color for the first 10 rows
            const arrowColor =
              index < 10
                ? priceDirection === "up"
                  ? "green"
                  : priceDirection === "down"
                  ? "red"
                  : "gray"
                : "gray";

            return (
              <tr key={item.symbolLevel || index}>
                <td>{index + 1}</td>
                <td style={{ textAlign: "center" }}>
                  <div className="quantity-bar bid">
                    <div
                      className="bar"
                      style={{
                        width: `${Math.min(Math.abs(bidChange), 100) * 35}%`, // Multiplied to make bars longer
                        backgroundColor: "rgba(0, 0, 255, 0.3)",
                        height: "50px",
                      }}
                    />
                    {item.bidQuantity}
                  </div>
                </td>
                <td style={{ textAlign: "center" }}>
                  <span
                    className={`arrow ${priceDirection}`}
                    style={{ color: arrowColor }}
                  >
                    {getDirectionIcon(priceDirection)}
                  </span>
                  {item.bid}
                </td>
                <td style={{ textAlign: "center" }}>
                  <span
                    className={`arrow ${offerDirection}`}
                    style={{
                      color:
                        index < 10
                          ? offerDirection === "up"
                            ? "green"
                            : offerDirection === "down"
                            ? "red"
                            : "gray"
                          : "gray",
                    }}
                  >
                    {getDirectionIcon(offerDirection)}
                  </span>
                  {item.offer}
                </td>
                <td className="ask-quantity-cell">
                  <div className="quantity-bar ask">
                    <div
                      className="bar"
                      style={{
                        width: `${Math.min(Math.abs(offerChange), 100) * 35}%`, // Multiplied to make bars longer
                        backgroundColor: "rgba(255, 0, 0, 0.3)",
                        height: "50px",
                      }}
                    />
                    {item.offerQuantity}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

export default MarketTable;
