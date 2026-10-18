import { Controller, Get, HttpStatus, Inject, Res } from '@nestjs/common'
import { DynamoDBClient, ListTablesCommand } from '@aws-sdk/client-dynamodb'
import type { Response } from 'express'

import { DYNAMODB_CLIENT } from '../common/dynamodb/dynamodb.constants.js'

/**
 * Health check en GET /health, mismo formato que el Actuator de los
 * servicios Spring ({ status: 'UP' | 'DOWN', components }). Endpoint de
 * infraestructura: va fuera de /api/v1 y sin el sobre ApiResponse.
 */
@Controller('health')
export class HealthController {
  constructor(@Inject(DYNAMODB_CLIENT) private readonly dynamo: DynamoDBClient) {}

  @Get()
  async health(@Res() res: Response): Promise<void> {
    let dynamodb: 'UP' | 'DOWN' = 'UP'
    try {
      await this.dynamo.send(new ListTablesCommand({ Limit: 1 }))
    } catch {
      dynamodb = 'DOWN'
    }
    const status = dynamodb
    res
      .status(status === 'UP' ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
      .json({ status, components: { dynamodb: { status: dynamodb } } })
  }
}
