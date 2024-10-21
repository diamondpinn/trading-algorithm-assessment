import { useState, useEffect } from "react";
import { TableSchema } from "@vuu-ui/vuu-data-types";
import { MarketDepthRow } from "../market-depth/useMarketDepthData";

// Define your useMarketData hook
export const useMarketData = (schema: TableSchema) => {
  const [data, setData] = useState<MarketDepthRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    const socket = new WebSocket("ws://localhost:8090/websocket");

    socket.onopen = () => {
      console.log("Connected to WebSocket server");
      console.log("Message received:");
      setLoading(false);
    };

    socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);

        // Handle market data messages
        if (message.type === "marketData") {
          const marketData: MarketDepthRow = {
            // Construct  market data row based on the received message
            bid: message.bid,
            bidQuantity: message.bidQuantity,
            level: message.level,
            offer: message.offer,
            offerQuantity: message.offerQuantity,
            symbolLevel: message.symbolLevel,
          };
          setData((prevData) => [...prevData, marketData]);
        }
      } catch (error) {
        console.error("Error parsing WebSocket data:", error);
        setError("Error parsing incoming data");
      }
    };

    socket.onerror = (event) => {
      console.error("WebSocket error observed:", event);
      setError("WebSocket error occurred");
    };

    socket.onclose = () => {
      console.log("WebSocket closed");
    };

    return () => {
      socket.close();
    };
  }, [schema]);

  return { data, loading, error };
};

// Defining useVuuMainMarketData hook
const useVuuMainMarketData = () => {
  const schema: TableSchema = {
    table: {
      table: "Trading_Table",
      module: "UI_Frontend",
    },
    columns: [
      { name: "symbolLevel", serverDataType: "string" },
      { name: "level", serverDataType: "long" },
      { name: "bid", serverDataType: "long" },
      { name: "bidQuantity", serverDataType: "long" },
      { name: "offer", serverDataType: "long" },
      { name: "offerQuantity", serverDataType: "long" },
    ],
    key: "symbolLevel",
  };

  const { data, loading, error } = useMarketData(schema);

  return { data, loading, error, schema };
};

export default useVuuMainMarketData;