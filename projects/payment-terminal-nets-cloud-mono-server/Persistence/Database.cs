using System;
using System.IO;
using System.Collections.Generic;
using Microsoft.Data.Sqlite;
using Newtonsoft.Json;
using PaymentTerminalNetsCloudMonoServer.Configuration;
using PaymentTerminalNetsCloudMonoServer.Models;

namespace PaymentTerminalNetsCloudMonoServer.Persistence
{
    public class Database : IDisposable
    {
        private readonly string _connectionString;
        private bool _initialized;

        public Database(ServerConfig config)
        {
            var dbPath = Path.GetFullPath(config.DatabasePath);
            var dbDir = Path.GetDirectoryName(dbPath);
            if (!string.IsNullOrWhiteSpace(dbDir))
                Directory.CreateDirectory(dbDir);
            _connectionString = $"Data Source={dbPath};";
            Initialize();
        }

        private void Initialize()
        {
            if (_initialized) return;
            using (var conn = new SqliteConnection(_connectionString))
            {
                conn.Open();
                CreateTables(conn);
                EnsureColumns(conn);
            }
            _initialized = true;
        }

        private void CreateTables(SqliteConnection conn)
        {
            var operationsTable = @"
                CREATE TABLE IF NOT EXISTS operations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    operationId TEXT NOT NULL UNIQUE,
                    clientRequestId TEXT,
                    operationType TEXT NOT NULL,
                    startedAt TEXT NOT NULL,
                    completedAt TEXT,
                    durationMs INTEGER,
                    callResult INTEGER,
                    methodRejectCode INTEGER,
                    methodRejectInfo TEXT,
                    resultEventName TEXT,
                    localModeResult INTEGER,
                    responseCode TEXT,
                    rejectionSource TEXT,
                    rejectionReason TEXT,
                    printTextRaw TEXT,
                    printTextSanitized TEXT,
                    displayText TEXT,
                    error TEXT,
                    receiptFileId TEXT,
                    createdAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )";
            var eventsTable = @"
                CREATE TABLE IF NOT EXISTS events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    eventId TEXT NOT NULL UNIQUE,
                    operationId TEXT,
                    timestamp TEXT NOT NULL,
                    eventType TEXT NOT NULL,
                    eventPayload TEXT,
                    createdAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )";
            var derivedFieldsTable = @"
                CREATE TABLE IF NOT EXISTS derived_fields (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    operationId TEXT NOT NULL,
                    fieldName TEXT NOT NULL,
                    fieldValue TEXT,
                    createdAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(operationId, fieldName)
                )";

            using (var cmd = new SqliteCommand(operationsTable, conn)) cmd.ExecuteNonQuery();
            using (var cmd = new SqliteCommand(eventsTable, conn)) cmd.ExecuteNonQuery();
            using (var cmd = new SqliteCommand(derivedFieldsTable, conn)) cmd.ExecuteNonQuery();

            var indexes = new[]
            {
                "CREATE INDEX IF NOT EXISTS idx_operations_clientRequestId ON operations(clientRequestId)",
                "CREATE INDEX IF NOT EXISTS idx_operations_startedAt ON operations(startedAt)",
                "CREATE INDEX IF NOT EXISTS idx_events_operationId ON events(operationId)",
                "CREATE INDEX IF NOT EXISTS idx_events_timestamp ON events(timestamp)",
                "CREATE INDEX IF NOT EXISTS idx_derived_fields_operationId ON derived_fields(operationId)"
            };
            foreach (var index in indexes)
            {
                using (var cmd = new SqliteCommand(index, conn)) cmd.ExecuteNonQuery();
            }
        }

        private void EnsureColumns(SqliteConnection conn)
        {
            TryAddColumn(conn, "operations", "errorCode", "TEXT");
            TryAddColumn(conn, "operations", "localModeFieldsJson", "TEXT");
            TryAddColumn(conn, "operations", "reportFieldsJson", "TEXT");
        }

        private static void TryAddColumn(SqliteConnection conn, string table, string column, string typeSql)
        {
            try
            {
                using (var cmd = new SqliteCommand($"ALTER TABLE {table} ADD COLUMN {column} {typeSql}", conn))
                    cmd.ExecuteNonQuery();
            }
            catch { }
        }

        public long SaveOperation(OperationRecord record)
        {
            using (var conn = new SqliteConnection(_connectionString))
            {
                conn.Open();
                var sql = @"
                    INSERT INTO operations (
                        operationId, clientRequestId, operationType, startedAt, completedAt, durationMs,
                        callResult, methodRejectCode, methodRejectInfo, resultEventName, localModeResult,
                        responseCode, rejectionSource, rejectionReason, printTextRaw, printTextSanitized,
                        displayText, error, receiptFileId, errorCode, localModeFieldsJson, reportFieldsJson
                    ) VALUES (
                        @operationId, @clientRequestId, @operationType, @startedAt, @completedAt, @durationMs,
                        @callResult, @methodRejectCode, @methodRejectInfo, @resultEventName, @localModeResult,
                        @responseCode, @rejectionSource, @rejectionReason, @printTextRaw, @printTextSanitized,
                        @displayText, @error, @receiptFileId, @errorCode, @localModeFieldsJson, @reportFieldsJson
                    )";

                using (var cmd = new SqliteCommand(sql, conn))
                {
                    cmd.Parameters.AddWithValue("@operationId", record.OperationId ?? "");
                    cmd.Parameters.AddWithValue("@clientRequestId", (object)record.ClientRequestId ?? (object)DBNull.Value);
                    cmd.Parameters.AddWithValue("@operationType", record.OperationType ?? "");
                    cmd.Parameters.AddWithValue("@startedAt", record.StartedAt.ToString("O"));
                    cmd.Parameters.AddWithValue("@completedAt", record.CompletedAt.HasValue ? (object)record.CompletedAt.Value.ToString("O") : (object)DBNull.Value);
                    cmd.Parameters.AddWithValue("@durationMs", record.DurationMs.HasValue ? (object)record.DurationMs.Value : (object)DBNull.Value);
                    cmd.Parameters.AddWithValue("@callResult", record.CallResult);
                    cmd.Parameters.AddWithValue("@methodRejectCode", record.MethodRejectCode);
                    cmd.Parameters.AddWithValue("@methodRejectInfo", (object)record.MethodRejectInfo ?? (object)DBNull.Value);
                    cmd.Parameters.AddWithValue("@resultEventName", (object)record.ResultEventName ?? (object)DBNull.Value);
                    cmd.Parameters.AddWithValue("@localModeResult", record.LocalModeResult);
                    cmd.Parameters.AddWithValue("@responseCode", (object)record.ResponseCode ?? (object)DBNull.Value);
                    cmd.Parameters.AddWithValue("@rejectionSource", (object)record.RejectionSource ?? (object)DBNull.Value);
                    cmd.Parameters.AddWithValue("@rejectionReason", (object)record.RejectionReason ?? (object)DBNull.Value);
                    cmd.Parameters.AddWithValue("@printTextRaw", (object)record.PrintTextRaw ?? (object)DBNull.Value);
                    cmd.Parameters.AddWithValue("@printTextSanitized", (object)record.PrintTextSanitized ?? (object)DBNull.Value);
                    cmd.Parameters.AddWithValue("@displayText", (object)record.DisplayText ?? (object)DBNull.Value);
                    cmd.Parameters.AddWithValue("@error", (object)record.Error ?? (object)DBNull.Value);
                    cmd.Parameters.AddWithValue("@receiptFileId", (object)record.ReceiptFileId ?? (object)DBNull.Value);
                    cmd.Parameters.AddWithValue("@errorCode", (object)record.ErrorCode ?? (object)DBNull.Value);
                    cmd.Parameters.AddWithValue("@localModeFieldsJson", (object)record.LocalModeFieldsJson ?? (object)DBNull.Value);
                    cmd.Parameters.AddWithValue("@reportFieldsJson", (object)record.ReportFieldsJson ?? (object)DBNull.Value);
                    cmd.ExecuteNonQuery();
                }

                using (var cmd = new SqliteCommand("SELECT last_insert_rowid()", conn))
                    return Convert.ToInt64(cmd.ExecuteScalar());
            }
        }

        public OperationResponse GetOperationByClientRequestId(string clientRequestId)
        {
            if (string.IsNullOrWhiteSpace(clientRequestId)) return null;

            using (var conn = new SqliteConnection(_connectionString))
            {
                conn.Open();
                var sql = @"
                    SELECT operationId, operationType, startedAt, completedAt, durationMs,
                        callResult, methodRejectCode, methodRejectInfo, resultEventName, localModeResult,
                        responseCode, rejectionSource, rejectionReason,
                        printTextRaw, printTextSanitized, displayText, error, receiptFileId,
                        errorCode, localModeFieldsJson, reportFieldsJson
                    FROM operations WHERE clientRequestId = @clientRequestId
                    ORDER BY id DESC LIMIT 1";

                using (var cmd = new SqliteCommand(sql, conn))
                {
                    cmd.Parameters.AddWithValue("@clientRequestId", clientRequestId.Trim());
                    using (var r = cmd.ExecuteReader())
                    {
                        if (!r.Read()) return null;

                        var startedAt = ParseUtc(r["startedAt"] as string);
                        var completedAt = ParseUtcNullable(r["completedAt"] as string);
                        var localModeFields = DeserializeMap(r["localModeFieldsJson"] as string);
                        var reportFields = DeserializeMap(r["reportFieldsJson"] as string);

                        return new OperationResponse
                        {
                            Success = string.IsNullOrWhiteSpace(r["errorCode"] as string) && string.IsNullOrWhiteSpace(r["error"] as string),
                            OperationId = r["operationId"] as string,
                            StartedAt = startedAt ?? DateTime.MinValue,
                            CompletedAt = completedAt,
                            DurationMs = ToNullableInt(r["durationMs"]),
                            CallResult = ToInt(r["callResult"]),
                            MethodRejectCode = ToInt(r["methodRejectCode"]),
                            MethodRejectInfo = r["methodRejectInfo"] as string,
                            ResultEventName = r["resultEventName"] as string,
                            LocalModeResult = ToInt(r["localModeResult"]),
                            ResponseCode = r["responseCode"] as string,
                            RejectionSource = r["rejectionSource"] as string,
                            RejectionReason = r["rejectionReason"] as string,
                            LocalModeFields = localModeFields,
                            PrintTextRaw = r["printTextRaw"] as string,
                            PrintTextSanitized = r["printTextSanitized"] as string,
                            LastDisplayText = r["displayText"] as string,
                            Error = r["error"] as string,
                            ErrorCode = r["errorCode"] as string,
                            ReceiptFileId = r["receiptFileId"] as string,
                            ReportFields = reportFields
                        };
                    }
                }
            }
        }

        private static DateTime? ParseUtc(string iso)
        {
            if (string.IsNullOrWhiteSpace(iso)) return null;
            if (DateTime.TryParse(iso, null, System.Globalization.DateTimeStyles.RoundtripKind, out var dt))
                return dt;
            return null;
        }

        private static DateTime? ParseUtcNullable(string iso) => ParseUtc(iso);

        private static int ToInt(object o)
        {
            if (o == null || o is DBNull) return 0;
            try { return Convert.ToInt32(o); } catch { return 0; }
        }

        private static int? ToNullableInt(object o)
        {
            if (o == null || o is DBNull) return null;
            try { return Convert.ToInt32(o); } catch { return null; }
        }

        private static Dictionary<string, string> DeserializeMap(string json)
        {
            if (string.IsNullOrWhiteSpace(json)) return null;
            try { return JsonConvert.DeserializeObject<Dictionary<string, string>>(json); }
            catch { return null; }
        }

        public void Dispose() { }
    }

    public class OperationRecord
    {
        public string OperationId { get; set; }
        public string ClientRequestId { get; set; }
        public string OperationType { get; set; }
        public DateTime StartedAt { get; set; }
        public DateTime? CompletedAt { get; set; }
        public int? DurationMs { get; set; }
        public int CallResult { get; set; }
        public int MethodRejectCode { get; set; }
        public string MethodRejectInfo { get; set; }
        public string ResultEventName { get; set; }
        public int LocalModeResult { get; set; }
        public string ResponseCode { get; set; }
        public string RejectionSource { get; set; }
        public string RejectionReason { get; set; }
        public string PrintTextRaw { get; set; }
        public string PrintTextSanitized { get; set; }
        public string DisplayText { get; set; }
        public string Error { get; set; }
        public string ErrorCode { get; set; }
        public string ReceiptFileId { get; set; }
        public string LocalModeFieldsJson { get; set; }
        public string ReportFieldsJson { get; set; }
    }
}
