#!/usr/bin/env python3
"""
Generate WireMock stub mappings from OpenAPI specification.

This script parses openapi-payment-terminal.yaml and generates WireMock
mapping files with example responses based on the schema definitions.

Useful for creating initial stubs without needing the actual payment terminal.
"""

import yaml
import json
import os
import sys
from pathlib import Path
from typing import Any, Dict, Optional

# Configuration
OPENAPI_FILE = "openapi-payment-terminal.yaml"
MAPPINGS_DIR = "wiremock/mappings"
OUTPUT_PREFIX = "stub-"


def load_openapi_spec() -> Dict[str, Any]:
    """Load and parse the OpenAPI specification."""
    if not os.path.exists(OPENAPI_FILE):
        print(f"Error: {OPENAPI_FILE} not found")
        sys.exit(1)
    
    with open(OPENAPI_FILE, 'r') as f:
        spec = yaml.safe_load(f)
    
    print(f"✓ Loaded OpenAPI spec: {spec['info']['title']} v{spec['info']['version']}")
    return spec


def resolve_ref(spec: Dict[str, Any], ref: str) -> Dict[str, Any]:
    """Resolve a $ref to its definition."""
    if not ref.startswith("#/"):
        raise ValueError(f"Only local refs supported: {ref}")
    
    parts = ref.split("/")[1:]  # Skip the leading #
    obj = spec
    for part in parts:
        obj = obj[part]
    return obj


def generate_example_from_schema(spec: Dict[str, Any], schema: Dict[str, Any]) -> Any:
    """Generate an example value from a JSON schema."""
    if "$ref" in schema:
        schema = resolve_ref(spec, schema["$ref"])
    
    schema_type = schema.get("type", "object")
    
    if "example" in schema:
        return schema["example"]
    
    if schema_type == "object":
        obj = {}
        properties = schema.get("properties", {})
        required = schema.get("required", [])
        
        for prop_name, prop_schema in properties.items():
            # Include if required or if not nullable
            if prop_name in required or not prop_schema.get("nullable", False):
                obj[prop_name] = generate_example_from_schema(spec, prop_schema)
            else:
                obj[prop_name] = None
        
        return obj
    
    elif schema_type == "array":
        items_schema = schema.get("items", {})
        return [generate_example_from_schema(spec, items_schema)]
    
    elif schema_type == "string":
        if "enum" in schema:
            return schema["enum"][0]
        if schema.get("format") == "date-time":
            return "2024-01-01T12:00:00Z"
        if schema.get("format") == "uuid":
            return "123e4567-e89b-12d3-a456-426614174000"
        return schema.get("example", "example-string")
    
    elif schema_type == "integer":
        return schema.get("example", 12345)
    
    elif schema_type == "number":
        return schema.get("example", 123.45)
    
    elif schema_type == "boolean":
        return schema.get("example", schema.get("default", True))
    
    else:
        return None


def generate_response_body(spec: Dict[str, Any], response_def: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    """Generate a response body from the OpenAPI response definition."""
    content = response_def.get("content", {})
    
    # Try JSON first
    if "application/json" in content:
        schema = content["application/json"].get("schema", {})
        return generate_example_from_schema(spec, schema)
    
    return None


def create_wiremock_mapping(
    name: str,
    method: str,
    url: str,
    request_body_schema: Optional[Dict[str, Any]],
    response_status: int,
    response_body: Optional[Dict[str, Any]],
    response_headers: Optional[Dict[str, str]] = None
) -> Dict[str, Any]:
    """Create a WireMock mapping definition."""
    mapping = {
        "name": name,
        "request": {
            "method": method.upper(),
            "url": url
        },
        "response": {
            "status": response_status,
            "headers": response_headers or {"Content-Type": "application/json"}
        }
    }
    
    # Add request body matcher if schema provided
    if request_body_schema:
        mapping["request"]["headers"] = {
            "Content-Type": {"matches": "application/json.*"}
        }
    
    # Add response body
    if response_body:
        mapping["response"]["jsonBody"] = response_body
    
    return mapping


def sanitize_filename(text: str) -> str:
    """Convert text to safe filename."""
    return text.lower().replace(" ", "-").replace("/", "-").replace(":", "")


def generate_mappings(spec: Dict[str, Any]) -> None:
    """Generate WireMock mappings from OpenAPI spec."""
    paths = spec.get("paths", {})
    
    # Create output directory
    os.makedirs(MAPPINGS_DIR, exist_ok=True)
    print(f"✓ Created output directory: {MAPPINGS_DIR}")
    
    mapping_count = 0
    
    for path, path_item in paths.items():
        for method, operation in path_item.items():
            if method not in ["get", "post", "put", "delete", "patch"]:
                continue
            
            operation_id = operation.get("operationId", f"{method}_{path}")
            summary = operation.get("summary", operation_id)
            
            # Get request body schema (if any)
            request_body_schema = None
            if "requestBody" in operation:
                content = operation["requestBody"].get("content", {})
                if "application/json" in content:
                    request_body_schema = content["application/json"].get("schema")
            
            # Process responses
            responses = operation.get("responses", {})
            
            for status_code, response_def in responses.items():
                if status_code.startswith("x-"):
                    continue
                
                # Check if this is a $ref to a common response
                if "$ref" in response_def:
                    response_def = resolve_ref(spec, response_def["$ref"])
                
                status_code_int = int(status_code)
                
                # Generate response body
                response_body = generate_response_body(spec, response_def)
                
                # Create mapping
                mapping_name = f"{summary} - {status_code}"
                mapping = create_wiremock_mapping(
                    name=mapping_name,
                    method=method,
                    url=path,
                    request_body_schema=request_body_schema,
                    response_status=status_code_int,
                    response_body=response_body
                )
                
                # Write to file
                filename = f"{OUTPUT_PREFIX}{sanitize_filename(operation_id)}-{status_code}.json"
                filepath = os.path.join(MAPPINGS_DIR, filename)
                
                with open(filepath, 'w') as f:
                    json.dump(mapping, f, indent=2)
                
                print(f"  ✓ {method.upper():6s} {path:40s} -> {status_code} ({filename})")
                mapping_count += 1
    
    print(f"\n✓ Generated {mapping_count} WireMock mappings")


def main():
    """Main entry point."""
    print("=== WireMock Stub Generator ===\n")
    
    # Load OpenAPI spec
    spec = load_openapi_spec()
    
    # Generate mappings
    print("\nGenerating WireMock mappings...\n")
    generate_mappings(spec)
    
    print("\n=== Complete ===")
    print(f"Mappings saved to: {MAPPINGS_DIR}/")
    print("\nTo run WireMock with these stubs:")
    print("  java -jar wiremock-standalone-3.3.1.jar --port 8080 --root-dir=./wiremock")


if __name__ == "__main__":
    main()
