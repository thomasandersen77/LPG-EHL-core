using System;
using System.Text.Json;
using FluentAssertions;
using pump_steering;
using Xunit;

namespace pump_steering.Tests
{
    public class IntegrationTests
    {
        [Fact]
        public void HappyPath_CompleteSetPriceFlow_ShouldParseValidateAndPrepare()
        {
            // Arrange - Complete JSON payload with all metadata
            var json = """
            {
              "cid": "b3b0c3df-5fbb-45e9-9f6a-0a3c1b5f62a1",
              "side": "AZURE_BACKEND",
              "actor": {
                "type": "USER",
                "id": "operator-42",
                "name": "John Doe"
              },
              "payload": {
                "price": 15.90
              }
            }
            """;

            // Act - Parse
            var (envelope, _) = ContractParser.ParseMethodRequest<SetPricePayload>(json);
            var cid = envelope.Cid;
            var side = envelope.Side;
            var actor = envelope.Actor;
            var priceValue = envelope.Payload.Price;

            // Assert Parse
            cid.Should().Be("b3b0c3df-5fbb-45e9-9f6a-0a3c1b5f62a1");
            side.Should().Be("AZURE_BACKEND");
            actor.Type.Should().Be("USER");
            actor.Id.Should().Be("operator-42");

            // Act - Validate price
            var priceValidation = ContractValidator.ValidatePrice(priceValue);

            // Assert Validation
            priceValidation.IsValid.Should().BeTrue();
            priceValue.Should().Be(15.90m);

            // Act - Validate all metadata
            var cidValidation = ContractValidator.ValidateCid(cid);
            var sideValidation = ContractValidator.ValidateSide(side);
            var actorValidation = ContractValidator.ValidateActor(actor);

            // Assert all validations pass
            cidValidation.IsValid.Should().BeTrue();
            sideValidation.IsValid.Should().BeTrue();
            actorValidation.IsValid.Should().BeTrue();

            // Act - Create success response
            var response = ContractParser.CreateResponse(
                cid,
                true,
                "Price updated successfully",
                data: new { new_price = priceValue }
            );

            // Assert Response
            response.Cid.Should().Be(cid, "Response should echo the correlation ID");
            response.Ok.Should().BeTrue();
            response.Message.Should().Be("Price updated successfully");
            response.ErrorCode.Should().BeNull();
            response.Data.Should().NotBeNull();

            // Verify response can be serialized back to JSON
            var responseJson = JsonSerializer.Serialize(response);
            responseJson.Should().Contain(cid);
            responseJson.Should().Contain("\"ok\":true");
        }

        [Fact]
        public void MethodResponseDto_SuccessAndError_ProduceCorrectShape()
        {
            var success = MethodResponseDto.Success("cid-1", "Done", new { x = 1 });
            success.Cid.Should().Be("cid-1");
            success.Ok.Should().BeTrue();
            success.Message.Should().Be("Done");
            success.ErrorCode.Should().BeNull();
            success.Data.Should().NotBeNull();

            var error = MethodResponseDto.Error("cid-2", "ERR", "Failed");
            error.Cid.Should().Be("cid-2");
            error.Ok.Should().BeFalse();
            error.ErrorCode.Should().Be("ERR");
            error.Message.Should().Be("Failed");
            error.Data.Should().BeNull();
        }

        [Fact]
        public void HappyPath_LegacyPayload_ShouldStillWork()
        {
            // Arrange - Old format without envelope
            var json = """
            {
              "price": 25.50
            }
            """;

            // Act - Parse with legacy handling
            var (envelope, isLegacy) = ContractParser.ParseMethodRequest<SetPricePayload>(json);
            var cid = envelope.Cid;
            var side = envelope.Side;
            var actor = envelope.Actor;

            // Assert - Should generate defaults
            isLegacy.Should().BeTrue();
            Guid.TryParse(cid, out _).Should().BeTrue("Should generate valid GUID");
            side.Should().Be("LEGACY");
            actor.Type.Should().Be("SYSTEM");
            actor.Id.Should().Be("legacy");

            // Act - Validate price
            var priceValue = envelope.Payload.Price;
            var priceValidation = ContractValidator.ValidatePrice(priceValue);

            // Assert
            priceValidation.IsValid.Should().BeTrue();
            priceValue.Should().Be(25.50m);

            // Response should still work with generated cid
            var response = ContractParser.CreateResponse(cid, true, "Success");
            response.Cid.Should().NotBeNullOrEmpty();
        }

        [Fact]
        public void SadPath_InvalidPrice_ShouldProduceErrorResponse()
        {
            // Arrange
            var json = """
            {
              "cid": "test-error-123",
              "side": "TEST",
              "actor": {
                "type": "SYSTEM",
                "id": "test"
              },
              "payload": {
                "price": 150.00
              }
            }
            """;

            // Act - Parse
            var (envelope, _) = ContractParser.ParseMethodRequest<SetPricePayload>(json);
            var cid = envelope.Cid;
            var priceValue = envelope.Payload.Price;

            // Act - Validate (should fail)
            var validation = ContractValidator.ValidatePrice(priceValue);

            // Assert validation fails
            validation.IsValid.Should().BeFalse();
            validation.ErrorCode.Should().Be("VALIDATION_ERROR");

            // Act - Create error response
            var response = ContractParser.CreateResponse(
                cid,
                false,
                validation.Message!,
                validation.ErrorCode
            );

            // Assert error response
            response.Cid.Should().Be("test-error-123");
            response.Ok.Should().BeFalse();
            response.ErrorCode.Should().Be("VALIDATION_ERROR");
            response.Message.Should().NotBeNullOrEmpty();
        }

        [Fact]
        public void CompleteFlow_UnlockCommand_WithMetadata()
        {
            // Arrange - Unlock command typically has no payload
            var json = """
            {
              "cid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
              "side": "STATION_OWNER_UI",
              "actor": {
                "type": "USER",
                "id": "station-owner-1",
                "name": "Station Manager"
              },
              "payload": {}
            }
            """;

            // Act
            var (envelope, _) = ContractParser.ParseMethodRequest<EmptyPayload>(json);
            var cid = envelope.Cid;
            var side = envelope.Side;
            var actor = envelope.Actor;

            // Assert metadata
            cid.Should().Be("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
            side.Should().Be("STATION_OWNER_UI");
            actor.Type.Should().Be("USER");

            // Validate metadata
            var cidVal = ContractValidator.ValidateCid(cid);
            var sideVal = ContractValidator.ValidateSide(side);
            var actorVal = ContractValidator.ValidateActor(actor);

            cidVal.IsValid.Should().BeTrue();
            sideVal.IsValid.Should().BeTrue();
            actorVal.IsValid.Should().BeTrue();

            // Create success response (simulate successful unlock)
            var response = ContractParser.CreateResponse(
                cid,
                true,
                "Pump unlocked",
                data: new { is_locked = false }
            );

            response.Cid.Should().Be(cid);
            response.Ok.Should().BeTrue();
        }

        [Fact]
        public void TelemetryData_ShouldIncludeMetadata()
        {
            // Arrange - After processing a command, we want to send telemetry
            var cid = "telemetry-test-789";
            var side = "ADMIN_UI";
            var actor = new Actor("SERVICE", "monitoring-service");

            // Act - Create telemetry envelope
            var telemetry = new TelemetryEnvelope(
                Cid: cid,
                Side: side,
                Actor: actor,
                Method: "SetPrice",
                Address: 33,
                Timestamp: DateTime.UtcNow,
                Command: 0xA9, // CMD_PROG_PRC
                Data: "30395315", // Encoded price data
                EventType: "EHL_RESPONSE"
            );

            // Assert
            telemetry.Cid.Should().Be(cid);
            telemetry.Side.Should().Be(side);
            telemetry.Actor.Should().Be(actor);
            telemetry.Method.Should().Be("SetPrice");
            telemetry.EventType.Should().Be("EHL_RESPONSE");

            // Verify it serializes correctly
            var json = JsonSerializer.Serialize(telemetry);
            json.Should().Contain(cid);
            json.Should().Contain("ADMIN_UI");
            json.Should().Contain("SetPrice");
        }
    }
}
