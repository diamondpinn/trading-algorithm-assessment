import { useState, useEffect } from 'react';
import { TableSchema } from "@vuu-ui/vuu-data-types"; // Import TableSchema
import { MarketDepthRow } from "../market-depth/useMarketDepthData"; // Import MarketDepthRow type

// Define your useMarketData hook
export const useMarketData = (schema: TableSchema) => {
  const [data, setData] = useState<MarketDepthRow[]>([]); // State for market data
  const [loading, setLoading] = useState(true); // Loading state
  const [error, setError] = useState<string | null>(null); // Error state

  useEffect(() => {
    const socket = new WebSocket("ws://localhost:8090/websocket"); // WebSocket connection

    socket.onopen = () => {
      console.log("Connected to WebSocket server");
      console.log("Message received:");
      setLoading(false); // Set loading to false when connected
    };

    socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data); // Parse the incoming message

        // Handle market data messages
        if (message.type === "marketData") {
          const marketData: MarketDepthRow = {
            // Construct your market data row based on the received message
            bid: message.bid,
            bidQuantity: message.bidQuantity,
            level: message.level,
            offer: message.offer,
            offerQuantity: message.offerQuantity,
            symbolLevel: message.symbolLevel,
          };
          setData((prevData) => [...prevData, marketData]); // Append new market data
        }
      } catch (error) {
        console.error("Error parsing WebSocket data:", error); // Log parsing errors
        setError("Error parsing incoming data");
      }
    };

    socket.onerror = (event) => {
      console.error("WebSocket error observed:", event);
      setError("WebSocket error occurred"); // Set error state on WebSocket errors
    };

    socket.onclose = () => {
      console.log("WebSocket closed");
    };

    // Cleanup on unmount
    return () => {
      socket.close(); // Close WebSocket connection
    };
  }, [schema]); // Dependency on schema if it changes

  return { data, loading, error }; // Return data, loading state, and error state
};

// Define your useVuuMainMarketData hook
const useVuuMainMarketData = () => {
  // Define the schema with the correct structure
  const schema: TableSchema = {
    table: {
      table: "Trading_Table", // Specify the actual table name here
      module: "UI_Frontend" // Specify the actual module name if applicable
    },
    columns: [
      { name: "symbolLevel", serverDataType: "string" }, // Server data type for each column
      { name: "level", serverDataType: "long" },
      { name: "bid", serverDataType: "long" },
      { name: "bidQuantity", serverDataType: "long" },
      { name: "offer", serverDataType: "long" },
      { name: "offerQuantity", serverDataType: "long" },
    ],
    key: "symbolLevel", // Correct identifier for your primary key
  };

  const { data, loading, error } = useMarketData(schema); // Use the market data hook

  return { data, loading, error, schema }; // Return data, loading state, error state, and schema
};

export default useVuuMainMarketData;
